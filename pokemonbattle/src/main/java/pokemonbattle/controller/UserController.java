package pokemonbattle.controller;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpSession;
import pokemonbattle.database.Database;

@RestController
@RequestMapping("/api/user")
public class UserController {

    // ENDPOINT: MENGAMBIL DATA RINGKASAN DASHBOARD HOME
    @GetMapping("/dashboard-stats")
    public Map<String, Object> getDashboardStats(HttpSession session) {
        Map<String, Object> stats = new HashMap<>();
        String username = (String) session.getAttribute("currentUser");

        if (username == null) {
            stats.put("success", false);
            stats.put("message", "Sesi berakhir atau user belum login.");
            return stats;
        }

        String sql = "SELECT u.id, u.wins, u.losses, "
                + "(SELECT COUNT(*) FROM user_pokemon up WHERE up.user_id = u.id) as total_pokemon, "
                + "(SELECT COALESCE(SUM(ui.quantity), 0) FROM user_inventory ui WHERE ui.user_id = u.id) as total_items "
                + "FROM users u WHERE u.username = ?";

        try (Connection conn = Database.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    stats.put("success", true);
                    stats.put("username", username);
                    stats.put("wins", rs.getInt("wins"));
                    stats.put("losses", rs.getInt("losses"));
                    stats.put("totalPokemon", rs.getInt("total_pokemon"));
                    stats.put("totalItems", rs.getInt("total_items"));
                } else {
                    stats.put("success", false);
                    stats.put("message", "Data user tidak ditemukan di database.");
                }
            }
        } catch (Exception e) {
            stats.put("success", false);
            stats.put("message", "Gagal memproses query dashboard: " + e.getMessage());
            e.printStackTrace();
        }

        return stats;
    }

    // ENDPOINT: MENGAMBIL DAFTAR KARTU POKEMON UNTUK MENU COLLECTION
    @GetMapping("/collection")
    public List<Map<String, Object>> getUserCollection(HttpSession session) {
        List<Map<String, Object>> collectionList = new ArrayList<>();
        String username = (String) session.getAttribute("currentUser");

        if (username == null) {
            return collectionList;
        }

        // UPDATE: Tambahkan p.speed ke dalam SELECT
        String sql = "SELECT up.pokemon_id as id, p.name, p.hp, p.max_hp, p.attack, p.defense, p.speed, p.rarity, t.name as type_name " +
             "FROM user_pokemon up " +
             "JOIN pokemon p ON up.pokemon_id = p.id " +
             "JOIN types t ON p.type_id = t.id " +
             "JOIN users u ON up.user_id = u.id " +
             "WHERE u.username = ?";

        try (Connection conn = Database.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> poke = new HashMap<>();
                    poke.put("id", rs.getInt("id"));
                    poke.put("name", rs.getString("name"));
                    poke.put("hp", rs.getInt("hp"));
                    poke.put("max_hp", rs.getInt("max_hp"));
                    poke.put("attack", rs.getInt("attack"));
                    poke.put("defense", rs.getInt("defense"));
                    poke.put("speed", rs.getInt("speed"));
                    poke.put("rarity", rs.getString("rarity"));
                    poke.put("type", rs.getString("type_name"));

                    collectionList.add(poke);
                }
            }
        } catch (Exception e) {
            System.err.println("Gagal menarik data koleksi Pokemon: " + e.getMessage());
            e.printStackTrace();
        }

        return collectionList;
    }

    //ENDPOINT: MENGAMBIL DAFTAR ITEM DI TAS (INVENTORY)
    @GetMapping("/inventory")
    public List<Map<String, Object>> getUserInventory(HttpSession session) {
        List<Map<String, Object>> inventoryList = new ArrayList<>();
        String username = (String) session.getAttribute("currentUser");

        if (username == null) {
            return inventoryList;
        }

        // Query relasional untuk mengambil item yang dimiliki user beserta jumlahnya (quantity)
        String sql = "SELECT i.name, i.description, i.effect_value, i.type, ui.quantity "
                + "FROM user_inventory ui "
                + "JOIN item i ON ui.item_id = i.id "
                + "JOIN users u ON ui.user_id = u.id "
                + "WHERE u.username = ?";

        try (Connection conn = Database.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> item = new HashMap<>();
                    item.put("name", rs.getString("name"));
                    item.put("description", rs.getString("description"));
                    item.put("effect_value", rs.getInt("effect_value"));
                    item.put("type", rs.getString("type"));
                    item.put("quantity", rs.getInt("quantity"));

                    inventoryList.add(item);
                }
            }
        } catch (Exception e) {
            System.err.println("Gagal menarik data inventory: " + e.getMessage());
            e.printStackTrace();
        }

        return inventoryList;
    }
}
