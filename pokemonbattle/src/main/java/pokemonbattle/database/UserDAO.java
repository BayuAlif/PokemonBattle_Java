package pokemonbattle.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import pokemonbattle.models.User;

public class UserDAO {

    public boolean registerUser(User user) {
        String insertUserSQL = "INSERT INTO users (username, password) VALUES (?, ?)";
        String getRandomPokemonSQL = "SELECT p.id FROM pokemon p JOIN types t ON p.type_id = t.id WHERE t.name = ? ORDER BY RANDOM() LIMIT 1";
        String givePokemonSQL = "INSERT INTO user_pokemon (user_id, pokemon_id) VALUES (?, ?)";
        
        // Query untuk mencari ID item dan memberikannya ke player
        String getItemIdSQL = "SELECT id FROM item WHERE name = ?";
        String giveItemSQL = "INSERT INTO user_inventory (user_id, item_id, quantity) VALUES (?, ?, ?)";

        try (Connection conn = Database.getConnection()) {
            if (conn == null) {
                System.err.println("Koneksi database null! Mencoba inisialisasi ulang...");
                InitDB.initialize();
                return false;
            }

            conn.setAutoCommit(false);
            int newUserId = -1;

            // 1. BUAT AKUN USER
            try (PreparedStatement pstmt = conn.prepareStatement(insertUserSQL, Statement.RETURN_GENERATED_KEYS)) {
                pstmt.setString(1, user.getUsername());
                pstmt.setString(2, user.getPassword());
                
                int affectedRows = pstmt.executeUpdate();
                if (affectedRows == 0) {
                    conn.rollback();
                    return false;
                }

                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        newUserId = generatedKeys.getInt(1);
                    } else {
                        conn.rollback();
                        return false;
                    }
                }
            }

            // 2. BAGIKAN 6 POKEMON STARTER
            String[] elements = {"FIRE", "WATER", "GRASS", "ELECTRIC", "ICE", "POISON"};
            try (PreparedStatement getPokeStmt = conn.prepareStatement(getRandomPokemonSQL);
                 PreparedStatement givePokeStmt = conn.prepareStatement(givePokemonSQL)) {
                
                for (String element : elements) {
                    getPokeStmt.setString(1, element);
                    try (ResultSet rs = getPokeStmt.executeQuery()) {
                        if (rs.next()) {
                            int randomPokemonId = rs.getInt("id");
                            givePokeStmt.setInt(1, newUserId);
                            givePokeStmt.setInt(2, randomPokemonId);
                            givePokeStmt.addBatch();
                        }
                    }
                }
                givePokeStmt.executeBatch();
            }

            // 3. BAGIKAN STARTER ITEMS KE INVENTORY (SESUAI REQUEST)
            Map<String, Integer> starterItems = new HashMap<>();
            starterItems.put("Potion", 15);
            starterItems.put("Mini Potion", 30);
            starterItems.put("Revive", 5);
            starterItems.put("Antidote", 10);

            try (PreparedStatement getItemStmt = conn.prepareStatement(getItemIdSQL);
                 PreparedStatement giveItemStmt = conn.prepareStatement(giveItemSQL)) {
                
                for (Map.Entry<String, Integer> entry : starterItems.entrySet()) {
                    getItemStmt.setString(1, entry.getKey());
                    try (ResultSet rsItem = getItemStmt.executeQuery()) {
                        if (rsItem.next()) {
                            int itemId = rsItem.getInt("id");
                            
                            giveItemStmt.setInt(1, newUserId);
                            giveItemStmt.setInt(2, itemId);
                            giveItemStmt.setInt(3, entry.getValue());
                            giveItemStmt.addBatch();
                        }
                    }
                }
                giveItemStmt.executeBatch();
            }

            // 4. SIMPAN PERMANEN
            conn.commit();
            conn.setAutoCommit(true);
            return true;

        } catch (Exception e) {
            System.err.println("Gagal Register & Memberikan Starter di DAO: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public User loginUser(String username, String password) {
        String sql = "SELECT * FROM users WHERE username = ? AND password = ?";
        try (Connection conn = Database.getConnection(); 
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            pstmt.setString(2, password);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    User loggedInUser = new User();
                    loggedInUser.setId(rs.getInt("id"));
                    loggedInUser.setUsername(rs.getString("username"));
                    loggedInUser.setPassword(rs.getString("password"));
                    return loggedInUser;
                }
            }
        } catch (Exception e) {
            System.err.println("Gagal Login Akun: " + e.getMessage());
        }
        return null;
    }
}