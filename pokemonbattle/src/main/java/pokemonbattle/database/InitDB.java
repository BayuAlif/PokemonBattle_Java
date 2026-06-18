package pokemonbattle.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

public class InitDB {
    public static void initialize() {
        // Query Tabel Pokemon
        String createPokemonTableSQL = "CREATE TABLE IF NOT EXISTS pokemon (" +
                                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                                "name TEXT, hp INTEGER, max_hp INTEGER, " +
                                "attack INTEGER, defense INTEGER, type TEXT);";
        
        // Query Tabel Users untuk Login & Register (BARU)
        String createUsersTableSQL = "CREATE TABLE IF NOT EXISTS users (" +
                                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                                "username TEXT UNIQUE, " +
                                "password TEXT);";
        
        // Query Tabel Item (Katalog semua barang di dalam game)
        String createItemTableSQL = "CREATE TABLE IF NOT EXISTS item (" +
                                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                                "name TEXT UNIQUE, " +
                                "description TEXT, " +
                                "effect_value INTEGER, " +
                                "type TEXT);"; // tipe item: HEAL, CATCH, dll

        // Query Tabel Inventory (Tas milik masing-masing user)
        String createInventoryTableSQL = "CREATE TABLE IF NOT EXISTS user_inventory (" +
                                "user_id INTEGER, " +
                                "item_id INTEGER, " +
                                "quantity INTEGER DEFAULT 0, " +
                                "PRIMARY KEY (user_id, item_id), " +
                                "FOREIGN KEY (user_id) REFERENCES users(id), " +
                                "FOREIGN KEY (item_id) REFERENCES item(id));";

        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement()) {
            
            stmt.execute("PRAGMA foreign_keys = ON;");//agar tas ndak nyasar ke user ghaib

            // Eksekusi pembuatan tabel
            stmt.execute(createPokemonTableSQL);
            stmt.execute(createUsersTableSQL);
            stmt.execute(createItemTableSQL);
            stmt.execute(createInventoryTableSQL);
            
            // Cek data seeding pokemon (agar tetap terisi 100 pokemon bawaanmu)
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM pokemon");
            if (rs.next() && rs.getInt(1) == 0) {
                System.out.println(">>> Mengisi database dengan 100 Pokémon awal... <<<");
                
                String insertSQL = "INSERT INTO pokemon (name, hp, max_hp, attack, defense, type) VALUES (?, ?, ?, ?, ?, ?)";
                
                try (PreparedStatement pstmt = conn.prepareStatement(insertSQL)) {
                    Object[][] pokemonData = {
                        // LEGENDARY & MYTHICAL
                        {"Entei", 190, 190, 135, 105, "FIRE"},
                        {"Ho-Oh", 185, 185, 150, 110, "FIRE"},
                        {"Suicune", 180, 180, 95, 135, "WATER"},
                        {"Kyogre", 180, 180, 120, 110, "WATER"},
                        {"Virizion", 170, 170, 110, 92, "GRASS"},
                        {"Zapdos", 170, 170, 110, 105, "ELECTRIC"},
                        {"Raikou", 170, 170, 105, 95, "ELECTRIC"},
                        {"Zekrom", 180, 180, 170, 140, "ELECTRIC"},
                        {"Thundurus", 160, 160, 135, 90, "ELECTRIC"},
                        {"Articuno", 170, 170, 105, 120, "ICE"},
                        {"Regice", 160, 160, 70, 150, "ICE"},
                        {"Kyurem", 200, 200, 150, 110, "ICE"},
                        
                        // TIPE FIRE
                        {"Charizard", 140, 140, 105, 100, "FIRE"},
                        {"Ninetales", 135, 135, 95, 95, "FIRE"},
                        {"Arcanine", 150, 150, 130, 100, "FIRE"},
                        {"Rapidash", 130, 130, 120, 90, "FIRE"},
                        {"Flareon", 130, 130, 150, 80, "FIRE"},
                        {"Typhlosion", 140, 140, 105, 100, "FIRE"},
                        {"Houndoom", 135, 135, 110, 70, "FIRE"},
                        {"Blaziken", 140, 140, 140, 90, "FIRE"},
                        {"Camerupt", 140, 140, 120, 90, "FIRE"},
                        {"Infernape", 135, 135, 125, 90, "FIRE"},
                        {"Magmortar", 140, 140, 115, 85, "FIRE"},
                        {"Emboar", 180, 180, 145, 85, "FIRE"},
                        {"Darmanitan", 170, 170, 160, 75, "FIRE"},
                        {"Chandelure", 125, 125, 75, 110, "FIRE"},
                        {"Volcarona", 150, 150, 80, 85, "FIRE"},
                        
                        // TIPE WATER
                        {"Blastoise", 140, 140, 105, 120, "WATER"},
                        {"Golduck", 140, 140, 100, 95, "WATER"},
                        {"Poliwrath", 150, 150, 115, 115, "WATER"},
                        {"Tentacruel", 140, 140, 90, 85, "WATER"},
                        {"Slowbro", 160, 160, 95, 130, "WATER"},
                        {"Kingler", 125, 125, 150, 135, "WATER"},
                        {"Gyarados", 160, 160, 145, 100, "WATER"},
                        {"Vaporeon", 200, 200, 85, 80, "WATER"},
                        {"Feraligatr", 150, 150, 125, 120, "WATER"},
                        {"Swampert", 170, 170, 130, 110, "WATER"},
                        {"Milotic", 160, 160, 80, 95, "WATER"},
                        {"Empoleon", 150, 150, 105, 105, "WATER"},
                        {"Samurott", 160, 160, 120, 105, "WATER"},
                        {"Seismitoad", 170, 170, 115, 95, "WATER"},
                        {"Jellicent", 170, 170, 80, 90, "WATER"},
                        
                        // TIPE GRASS
                        {"Venusaur", 140, 140, 100, 100, "GRASS"},
                        {"Vileplume", 135, 135, 100, 105, "GRASS"},
                        {"Victreebel", 140, 140, 125, 85, "GRASS"},
                        {"Exeggutor", 160, 160, 115, 105, "GRASS"},
                        {"Meganium", 140, 140, 100, 120, "GRASS"},
                        {"Bellossom", 135, 135, 100, 115, "GRASS"},
                        {"Sceptile", 135, 135, 105, 85, "GRASS"},
                        {"Ludicolo", 140, 140, 90, 90, "GRASS"},
                        {"Shiftry", 150, 150, 120, 80, "GRASS"},
                        {"Torterra", 160, 160, 125, 125, "GRASS"},
                        {"Roserade", 125, 125, 90, 85, "GRASS"},
                        {"Leafeon", 130, 130, 130, 150, "GRASS"},
                        {"Serperior", 135, 135, 95, 115, "GRASS"},
                        {"Leavanny", 135, 135, 125, 100, "GRASS"},
                        {"Lilligant", 135, 135, 80, 95, "GRASS"},
                        {"Amoonguss", 180, 180, 105, 90, "GRASS"},
                        
                        // TIPE ELECTRIC
                        {"Raichu", 125, 125, 110, 75, "ELECTRIC"},
                        {"Electrode", 125, 125, 70, 90, "ELECTRIC"},
                        {"Jolteon", 130, 130, 85, 80, "ELECTRIC"},
                        {"Ampharos", 150, 150, 95, 105, "ELECTRIC"},
                        {"Manectric", 135, 135, 95, 80, "ELECTRIC"},
                        {"Luxray", 140, 140, 140, 95, "ELECTRIC"},
                        {"Electivire", 140, 140, 145, 85, "ELECTRIC"},
                        {"Magnezone", 135, 135, 90, 135, "ELECTRIC"},
                        {"Zebstrika", 135, 135, 120, 80, "ELECTRIC"},
                        {"Galvantula", 135, 135, 95, 80, "ELECTRIC"},
                        {"Eelektross", 150, 150, 135, 100, "ELECTRIC"},
                        {"Stunfisk", 170, 170, 85, 100, "ELECTRIC"},
                        {"Emolga", 120, 120, 95, 80, "ELECTRIC"},
                        
                        // TIPE ICE
                        {"Dewgong", 150, 150, 90, 100, "ICE"},
                        {"Cloyster", 115, 115, 115, 180, "ICE"},
                        {"Jynx", 130, 130, 70, 55, "ICE"},
                        {"Lapras", 200, 200, 105, 100, "ICE"},
                        {"Walrein", 180, 180, 100, 110, "ICE"},
                        {"Glalie", 140, 140, 100, 100, "ICE"},
                        {"Abomasnow", 150, 150, 110, 95, "ICE"},
                        {"Weavile", 135, 135, 140, 85, "ICE"},
                        {"Glaceon", 130, 130, 80, 130, "ICE"},
                        {"Froslass", 135, 135, 100, 90, "ICE"},
                        {"Mamoswine", 180, 180, 150, 100, "ICE"},
                        {"Vanilluxe", 135, 135, 115, 105, "ICE"},
                        {"Beartic", 160, 160, 150, 100, "ICE"},
                        
                        // TIPE POISON
                        {"Arbok", 125, 125, 105, 85, "POISON"},
                        {"Nidoqueen", 150, 150, 110, 105, "POISON"},
                        {"Nidoking", 145, 145, 120, 95, "POISON"},
                        {"Muk", 170, 170, 125, 95, "POISON"},
                        {"Weezing", 130, 130, 110, 140, "POISON"},
                        {"Gengar", 125, 125, 85, 80, "POISON"},
                        {"Crobat", 150, 150, 110, 100, "POISON"},
                        {"Ariados", 135, 135, 110, 90, "POISON"},
                        {"Qwilfish", 130, 130, 115, 105, "POISON"},
                        {"Swalot", 170, 170, 95, 100, "POISON"},
                        {"Seviper", 135, 135, 120, 80, "POISON"},
                        {"Skuntank", 170, 170, 113, 87, "POISON"},
                        {"Drapion", 135, 135, 110, 130, "POISON"},
                        {"Toxicroak", 145, 145, 126, 85, "POISON"},
                        {"Garbodor", 150, 150, 115, 102, "POISON"},
                        {"Scolipede", 150, 150, 120, 109, "POISON"}
                    };
                    
                    for (Object[] p : pokemonData) {
                        pstmt.setString(1, (String) p[0]);
                        pstmt.setInt(2, (int) p[1]);
                        pstmt.setInt(3, (int) p[2]);
                        pstmt.setInt(4, (int) p[3]);
                        pstmt.setInt(5, (int) p[4]);
                        pstmt.setString(6, (String) p[5]);
                        pstmt.addBatch();
                    }
                    pstmt.executeBatch();
                }
                System.out.println(">>> Sukses menyuntikkan 100 Pokémon ke pokemon_game.db! <<<");
            } else {
                System.out.println(">>> Database sudah terisi, melewati proses seeding. <<<");
            }

            // SEEDING DATA ITEM (MASTER BARANG SAJA)
            ResultSet rsItem = stmt.executeQuery("SELECT COUNT(*) FROM item");
            if (rsItem.next() && rsItem.getInt(1) == 0) {
                System.out.println(">>> Mengisi database dengan 3 Item dasar... <<<");
                
                String insertItemSQL = "INSERT INTO item (name, description, effect_value, type) VALUES (?, ?, ?, ?)";
                
                try (PreparedStatement pstmtItem = conn.prepareStatement(insertItemSQL)) {
                    Object[][] itemData = {
                        {"Potion", "Mengembalikan 50 HP", 50, "HEAL"},
                        {"Pokeball", "Bola standar untuk menangkap Pokemon", 1, "CATCH"},
                        {"Revive", "Menghidupkan Pokemon yang pingsan", 50, "REVIVE"}
                    };
                    
                    for (Object[] i : itemData) {
                        pstmtItem.setString(1, (String) i[0]);
                        pstmtItem.setString(2, (String) i[1]);
                        pstmtItem.setInt(3, (int) i[2]);
                        pstmtItem.setString(4, (String) i[3]);
                        pstmtItem.addBatch();
                    }
                    pstmtItem.executeBatch();
                }
                System.out.println(">>> Sukses menyuntikkan 3 Item ke database! <<<");
            } else {
                System.out.println(">>> Database Item sudah terisi, melewati proses seeding. <<<");
            }
            
            System.out.println(">>> SQLite & Struktur Tabel Users Berhasil Diaktifkan! <<<");
        } catch (Exception e) {
            System.err.println("Gagal menginisialisasi database: " + e.getMessage());
            e.printStackTrace();
        }
    }
}