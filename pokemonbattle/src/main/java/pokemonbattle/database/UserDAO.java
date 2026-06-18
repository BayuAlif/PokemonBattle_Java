package pokemonbattle.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import pokemonbattle.models.User;

public class UserDAO {

    // LOGIC REGISTER: Simpan user baru ke SQLite
    public boolean registerUser(User user) {
        String sql = "INSERT INTO users (username, password) VALUES (?, ?)";
        try (Connection conn = Database.getConnection()) {
            if (conn == null) {
                System.err.println("Koneksi database null! Mencoba inisialisasi ulang...");
                InitDB.initialize();
                return false;
            }

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, user.getUsername());
                pstmt.setString(2, user.getPassword());

                int rows = pstmt.executeUpdate();
                return rows > 0;
            }
        } catch (Exception e) {
            System.err.println("Gagal Register Akun di DAO: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // LOGIC LOGIN: Cek keaslian username dan password di database
    public User loginUser(String username, String password) {
        String sql = "SELECT * FROM users WHERE username = ? AND password = ?";
        try (Connection conn = Database.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            pstmt.setString(2, password);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    User user = new User();
                    user.setId(rs.getInt("id"));
                    user.setUsername(rs.getString("username"));
                    user.setPassword(rs.getString("password"));
                    return user;
                }
            }
        } catch (Exception e) {
            System.err.println("Gagal Login Akun: " + e.getMessage());
        }
        return null; // Return null jika tidak cocok / gagal login
    }
}
