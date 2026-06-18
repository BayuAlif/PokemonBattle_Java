package pokemonbattle.database;

import java.sql.Connection;
import java.sql.Statement;

public class InitDB {
    public static void initialize() {
        String createTableSQL = "CREATE TABLE IF NOT EXISTS pokemon (" +
                                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                                "name TEXT, hp INTEGER, max_hp INTEGER, " +
                                "attack INTEGER, defense INTEGER, type TEXT);";
        
        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement()) {
            
            stmt.execute(createTableSQL);
            
            String insertSeed = "INSERT INTO pokemon (name, hp, max_hp, attack, defense, type) " +
                                "SELECT 'Pikachu', 100, 100, 20, 5, 'ELECTRIC' " +
                                "WHERE NOT EXISTS (SELECT 1 FROM pokemon WHERE name = 'Pikachu');";
            stmt.execute(insertSeed);
            
            System.out.println(">>> SQLite Berhasil Diinisialisasi & File pokemon_game.db Telah Aktif! <<<");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}