package pokemonbattle.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InitDB {
    public static void initialize() {
        
        String createTypesTableSQL = "CREATE TABLE IF NOT EXISTS types (" +
                                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                                "name TEXT UNIQUE);";

        String createUsersTableSQL = "CREATE TABLE IF NOT EXISTS users (" +
                                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                                "username TEXT UNIQUE, " +
                                "password TEXT, " +
                                "wins INTEGER DEFAULT 0, " +
                                "losses INTEGER DEFAULT 0);";

        String createPokemonTableSQL = "CREATE TABLE IF NOT EXISTS pokemon (" +
                                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                                "name TEXT, hp INTEGER, max_hp INTEGER, " +
                                "attack INTEGER, defense INTEGER, speed INTEGER, " +
                                "type_id INTEGER, " +
                                "rarity TEXT, " +
                                "FOREIGN KEY (type_id) REFERENCES types(id));";

        String createUserPokemonTableSQL = "CREATE TABLE IF NOT EXISTS user_pokemon (" +
                                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                                "user_id INTEGER, " +
                                "pokemon_id INTEGER, " +
                                "FOREIGN KEY (user_id) REFERENCES users(id), " +
                                "FOREIGN KEY (pokemon_id) REFERENCES pokemon(id));";

        String createSkillTableSQL = "CREATE TABLE IF NOT EXISTS skill (" +
                                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                                "name TEXT UNIQUE, " +
                                "type_id INTEGER, " +
                                "power INTEGER, " +
                                "accuracy INTEGER, " +
                                "FOREIGN KEY (type_id) REFERENCES types(id));";

        String createItemTableSQL = "CREATE TABLE IF NOT EXISTS item (" +
                                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                                "name TEXT UNIQUE, " +
                                "description TEXT, " +
                                "effect_value INTEGER, " +
                                "type TEXT);";

        String createInventoryTableSQL = "CREATE TABLE IF NOT EXISTS user_inventory (" +
                                "user_id INTEGER, " +
                                "item_id INTEGER, " +
                                "quantity INTEGER DEFAULT 0, " +
                                "PRIMARY KEY (user_id, item_id), " +
                                "FOREIGN KEY (user_id) REFERENCES users(id), " +
                                "FOREIGN KEY (item_id) REFERENCES item(id));";

        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement()) {
            
            stmt.execute("PRAGMA foreign_keys = ON;"); 

            stmt.execute(createTypesTableSQL);
            stmt.execute(createUsersTableSQL);
            stmt.execute(createPokemonTableSQL);
            stmt.execute(createUserPokemonTableSQL);
            stmt.execute(createSkillTableSQL);
            stmt.execute(createItemTableSQL);
            stmt.execute(createInventoryTableSQL);

            // ==========================================
            // SEEDING DATA: TABEL TYPES (ELEMEN)
            // ==========================================
            ResultSet rsTypes = stmt.executeQuery("SELECT COUNT(*) FROM types");
            if (rsTypes.next() && rsTypes.getInt(1) == 0) {
                System.out.println(">>> Mengisi database dengan Master Tipe Elemen... <<<");
                String[] typeNames = {"FIRE", "WATER", "GRASS", "ELECTRIC", "ICE", "POISON"};
                try (PreparedStatement pt = conn.prepareStatement("INSERT INTO types (name) VALUES (?)")) {
                    for (String t : typeNames) {
                        pt.setString(1, t);
                        pt.executeUpdate();
                    }
                }
            }

            Map<String, Integer> typeMap = new HashMap<>();
            try (ResultSet rs = stmt.executeQuery("SELECT id, name FROM types")) {
                while (rs.next()) {
                    typeMap.put(rs.getString("name"), rs.getInt("id"));
                }
            }

            // ==========================================
            // SEEDING DATA: POKEMON (DENGAN RARITY & SPEED)
            // ==========================================
            ResultSet rsPoke = stmt.executeQuery("SELECT COUNT(*) FROM pokemon");
            if (rsPoke.next() && rsPoke.getInt(1) == 0) {
                System.out.println(">>> Mengisi database dengan 100 Pokémon awal... <<<");
                String insertSQL = "INSERT INTO pokemon (name, hp, max_hp, attack, defense, speed, type_id, rarity) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
                
                List<String> legendaries = Arrays.asList(
                    "Entei", "Ho-Oh", "Suicune", "Kyogre", "Virizion", "Zapdos", 
                    "Raikou", "Zekrom", "Thundurus", "Articuno", "Regice", "Kyurem"
                );

                try (PreparedStatement pstmt = conn.prepareStatement(insertSQL)) {
                    Object[][] pokemonData = {
                        // LEGENDARY & MYTHICAL
                        {"Entei", 190, 190, 135, 105, 100, "FIRE"}, {"Ho-Oh", 185, 185, 150, 110, 90, "FIRE"},
                        {"Suicune", 180, 180, 95, 135, 85, "WATER"}, {"Kyogre", 180, 180, 120, 110, 90, "WATER"},
                        {"Virizion", 170, 170, 110, 92, 108, "GRASS"}, {"Zapdos", 170, 170, 110, 105, 100, "ELECTRIC"},
                        {"Raikou", 170, 170, 105, 95, 115, "ELECTRIC"}, {"Zekrom", 180, 180, 170, 140, 90, "ELECTRIC"},
                        {"Thundurus", 160, 160, 135, 90, 111, "ELECTRIC"}, {"Articuno", 170, 170, 105, 120, 85, "ICE"},
                        {"Regice", 160, 160, 70, 150, 50, "ICE"}, {"Kyurem", 200, 200, 150, 110, 95, "ICE"},
                        
                        // TIPE FIRE
                        {"Charizard", 140, 140, 105, 100, 100, "FIRE"}, {"Ninetales", 135, 135, 95, 95, 100, "FIRE"},
                        {"Arcanine", 150, 150, 130, 100, 95, "FIRE"}, {"Rapidash", 130, 130, 120, 90, 105, "FIRE"},
                        {"Flareon", 130, 130, 150, 80, 65, "FIRE"}, {"Typhlosion", 140, 140, 105, 100, 100, "FIRE"},
                        {"Houndoom", 135, 135, 110, 70, 95, "FIRE"}, {"Blaziken", 140, 140, 140, 90, 80, "FIRE"},
                        {"Camerupt", 140, 140, 120, 90, 40, "FIRE"}, {"Infernape", 135, 135, 125, 90, 108, "FIRE"},
                        {"Magmortar", 140, 140, 115, 85, 83, "FIRE"}, {"Emboar", 180, 180, 145, 85, 65, "FIRE"},
                        {"Darmanitan", 170, 170, 160, 75, 95, "FIRE"}, {"Chandelure", 125, 125, 75, 110, 80, "FIRE"},
                        {"Volcarona", 150, 150, 80, 85, 100, "FIRE"},
                        
                        // TIPE WATER
                        {"Blastoise", 140, 140, 105, 120, 78, "WATER"}, {"Golduck", 140, 140, 100, 95, 85, "WATER"},
                        {"Poliwrath", 150, 150, 115, 115, 70, "WATER"}, {"Tentacruel", 140, 140, 90, 85, 100, "WATER"},
                        {"Slowbro", 160, 160, 95, 130, 30, "WATER"}, {"Kingler", 125, 125, 150, 135, 75, "WATER"},
                        {"Gyarados", 160, 160, 145, 100, 81, "WATER"}, {"Vaporeon", 200, 200, 85, 80, 65, "WATER"},
                        {"Feraligatr", 150, 150, 125, 120, 78, "WATER"}, {"Swampert", 170, 170, 130, 110, 60, "WATER"},
                        {"Milotic", 160, 160, 80, 95, 81, "WATER"}, {"Empoleon", 150, 150, 105, 105, 60, "WATER"},
                        {"Samurott", 160, 160, 120, 105, 70, "WATER"}, {"Seismitoad", 170, 170, 115, 95, 74, "WATER"},
                        {"Jellicent", 170, 170, 80, 90, 60, "WATER"},
                        
                        // TIPE GRASS
                        {"Venusaur", 140, 140, 100, 100, 80, "GRASS"}, {"Vileplume", 135, 135, 100, 105, 50, "GRASS"},
                        {"Victreebel", 140, 140, 125, 85, 70, "GRASS"}, {"Exeggutor", 160, 160, 115, 105, 55, "GRASS"},
                        {"Meganium", 140, 140, 100, 120, 80, "GRASS"}, {"Bellossom", 135, 135, 100, 115, 50, "GRASS"},
                        {"Sceptile", 135, 135, 105, 85, 120, "GRASS"}, {"Ludicolo", 140, 140, 90, 90, 70, "GRASS"},
                        {"Shiftry", 150, 150, 120, 80, 80, "GRASS"}, {"Torterra", 160, 160, 125, 125, 56, "GRASS"},
                        {"Roserade", 125, 125, 90, 85, 90, "GRASS"}, {"Leafeon", 130, 130, 130, 150, 95, "GRASS"},
                        {"Serperior", 135, 135, 95, 115, 113, "GRASS"}, {"Leavanny", 135, 135, 125, 100, 92, "GRASS"},
                        {"Lilligant", 135, 135, 80, 95, 90, "GRASS"}, {"Amoonguss", 180, 180, 105, 90, 30, "GRASS"},
                        
                        // TIPE ELECTRIC
                        {"Raichu", 125, 125, 110, 75, 110, "ELECTRIC"}, {"Electrode", 125, 125, 70, 90, 150, "ELECTRIC"},
                        {"Jolteon", 130, 130, 85, 80, 130, "ELECTRIC"}, {"Ampharos", 150, 150, 95, 105, 55, "ELECTRIC"},
                        {"Manectric", 135, 135, 95, 80, 105, "ELECTRIC"}, {"Luxray", 140, 140, 140, 95, 70, "ELECTRIC"},
                        {"Electivire", 140, 140, 145, 85, 95, "ELECTRIC"}, {"Magnezone", 135, 135, 90, 135, 60, "ELECTRIC"},
                        {"Zebstrika", 135, 135, 120, 80, 116, "ELECTRIC"}, {"Galvantula", 135, 135, 95, 80, 108, "ELECTRIC"},
                        {"Eelektross", 150, 150, 135, 100, 50, "ELECTRIC"}, {"Stunfisk", 170, 170, 85, 100, 32, "ELECTRIC"},
                        {"Emolga", 120, 120, 95, 80, 103, "ELECTRIC"},
                        
                        // TIPE ICE
                        {"Dewgong", 150, 150, 90, 100, 70, "ICE"}, {"Cloyster", 115, 115, 115, 180, 70, "ICE"},
                        {"Jynx", 130, 130, 70, 55, 95, "ICE"}, {"Lapras", 200, 200, 105, 100, 60, "ICE"},
                        {"Walrein", 180, 180, 100, 110, 65, "ICE"}, {"Glalie", 140, 140, 100, 100, 80, "ICE"},
                        {"Abomasnow", 150, 150, 110, 95, 60, "ICE"}, {"Weavile", 135, 135, 140, 85, 125, "ICE"},
                        {"Glaceon", 130, 130, 80, 130, 65, "ICE"}, {"Froslass", 135, 135, 100, 90, 110, "ICE"},
                        {"Mamoswine", 180, 180, 150, 100, 80, "ICE"}, {"Vanilluxe", 135, 135, 115, 105, 79, "ICE"},
                        {"Beartic", 160, 160, 150, 100, 50, "ICE"},
                        
                        // TIPE POISON
                        {"Arbok", 125, 125, 105, 85, 80, "POISON"}, {"Nidoqueen", 150, 150, 110, 105, 76, "POISON"},
                        {"Nidoking", 145, 145, 120, 95, 85, "POISON"}, {"Muk", 170, 170, 125, 95, 50, "POISON"},
                        {"Weezing", 130, 130, 110, 140, 60, "POISON"}, {"Gengar", 125, 125, 85, 80, 110, "POISON"},
                        {"Crobat", 150, 150, 110, 100, 130, "POISON"}, {"Ariados", 135, 135, 110, 90, 40, "POISON"},
                        {"Qwilfish", 130, 130, 115, 105, 85, "POISON"}, {"Swalot", 170, 170, 95, 100, 55, "POISON"},
                        {"Seviper", 135, 135, 120, 80, 65, "POISON"}, {"Skuntank", 170, 170, 113, 87, 84, "POISON"},
                        {"Drapion", 135, 135, 110, 130, 95, "POISON"}, {"Toxicroak", 145, 145, 126, 85, 85, "POISON"},
                        {"Garbodor", 150, 150, 115, 102, 75, "POISON"}, {"Scolipede", 150, 150, 120, 109, 112, "POISON"}
                    };
                    
                    for (Object[] p : pokemonData) {
                        String pokeName = (String) p[0];
                        pstmt.setString(1, pokeName);
                        pstmt.setInt(2, (int) p[1]);
                        pstmt.setInt(3, (int) p[2]);
                        pstmt.setInt(4, (int) p[3]);
                        pstmt.setInt(5, (int) p[4]);
                        pstmt.setInt(6, (int) p[5]);
                        pstmt.setInt(7, typeMap.get((String) p[6]));
                        
                        if (legendaries.contains(pokeName)) {
                            pstmt.setString(8, "LEGENDARY");
                        } else {
                            pstmt.setString(8, "NORMAL");
                        }
                        pstmt.addBatch();
                    }
                    pstmt.executeBatch();
                }
            }

            // ==========================================
            // SEEDING DATA: ITEM (UPDATE: POKEBALL DIHAPUS, ANTIDOTE MASUK)
            // ==========================================
            ResultSet rsItem = stmt.executeQuery("SELECT COUNT(*) FROM item");
            if (rsItem.next() && rsItem.getInt(1) == 0) {
                System.out.println(">>> Mengisi database dengan Master Item Baru... <<<");
                String insertItemSQL = "INSERT INTO item (name, description, effect_value, type) VALUES (?, ?, ?, ?)";
                try (PreparedStatement pstmtItem = conn.prepareStatement(insertItemSQL)) {
                    Object[][] itemData = {
                        {"Potion", "Mengembalikan 50 HP", 50, "HEAL"},
                        {"Mini Potion", "Mengembalikan 20 HP", 20, "HEAL"},
                        {"Revive", "Menghidupkan Pokemon yang pingsan", 50, "REVIVE"},
                        {"Antidote", "Menyembuhkan status negatif seperti Poison", 0, "CURE"}
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
            }

            // ==========================================
            // SEEDING DATA: SKILL / MOVESET
            // ==========================================
            ResultSet rsSkill = stmt.executeQuery("SELECT COUNT(*) FROM skill");
            if (rsSkill.next() && rsSkill.getInt(1) == 0) {
                System.out.println(">>> Mengisi database dengan 120 Moveset ber-relasi... <<<");
                String insertSkillSQL = "INSERT INTO skill (name, type_id, power, accuracy) VALUES (?, ?, ?, ?)";
                try (PreparedStatement pstmtSkill = conn.prepareStatement(insertSkillSQL)) {
                    Object[][] skillData = {
                        {"Ember","FIRE",40,100}, {"Flamethrower","FIRE",90,100}, {"Fire Blast","FIRE",110,85}, 
                        {"Flame Wheel","FIRE",60,100}, {"Fire Punch","FIRE",75,100}, {"Flare Blitz","FIRE",120,100}, 
                        {"Heat Wave","FIRE",95,90}, {"Overheat","FIRE",130,90}, {"Fire Fang","FIRE",65,95}, 
                        {"Flame Charge","FIRE",50,100}, {"Lava Plume","FIRE",80,100}, {"Magma Storm","FIRE",100,75}, 
                        {"Blast Burn","FIRE",150,90}, {"Eruption","FIRE",150,100}, {"Mystical Fire","FIRE",75,100}, 
                        {"Incinerate","FIRE",60,100}, {"Inferno","FIRE",100,50}, {"Flame Burst","FIRE",70,100}, 
                        {"Sacred Fire","FIRE",100,95}, {"V-create","FIRE",180,95},
                        {"Water Gun","WATER",40,100}, {"Water Pulse","WATER",60,100}, {"Bubble Beam","WATER",65,100}, 
                        {"Surf","WATER",90,100}, {"Hydro Pump","WATER",110,80}, {"Waterfall","WATER",80,100}, 
                        {"Aqua Jet","WATER",40,100}, {"Aqua Tail","WATER",90,90}, {"Dive","WATER",80,100}, 
                        {"Crabhammer","WATER",100,90}, {"Clamp","WATER",35,85}, {"Whirlpool","WATER",35,85}, 
                        {"Water Spout","WATER",150,100}, {"Muddy Water","WATER",90,85}, {"Brine","WATER",65,100}, 
                        {"Scald","WATER",80,100}, {"Origin Pulse","WATER",110,85}, {"Steam Eruption","WATER",110,95}, 
                        {"Razor Shell","WATER",75,95}, {"Snipe Shot","WATER",80,100},
                        {"Vine Whip","GRASS",45,100}, {"Razor Leaf","GRASS",55,95}, {"Solar Beam","GRASS",120,100}, 
                        {"Giga Drain","GRASS",75,100}, {"Magical Leaf","GRASS",60,100}, {"Bullet Seed","GRASS",25,100}, 
                        {"Seed Bomb","GRASS",80,100}, {"Wood Hammer","GRASS",120,100}, {"Leaf Blade","GRASS",90,100}, 
                        {"Energy Ball","GRASS",90,100}, {"Leaf Storm","GRASS",130,90}, {"Petal Dance","GRASS",120,100}, 
                        {"Frenzy Plant","GRASS",150,90}, {"Grass Knot","GRASS",80,100}, {"Power Whip","GRASS",120,85}, 
                        {"Trop Kick","GRASS",70,100}, {"Solar Blade","GRASS",125,100}, {"Apple Acid","GRASS",80,100}, 
                        {"Drum Beating","GRASS",80,100}, {"Leaf Tornado","GRASS",65,90},
                        {"Thunder Shock","ELECTRIC",40,100}, {"Thunderbolt","ELECTRIC",90,100}, {"Thunder","ELECTRIC",110,70}, 
                        {"Volt Tackle","ELECTRIC",120,100}, {"Spark","ELECTRIC",65,100}, {"Thunder Punch","ELECTRIC",75,100}, 
                        {"Shock Wave","ELECTRIC",60,100}, {"Discharge","ELECTRIC",80,100}, {"Charge Beam","ELECTRIC",50,90}, 
                        {"Wild Charge","ELECTRIC",90,100}, {"Zap Cannon","ELECTRIC",120,50}, {"Volt Switch","ELECTRIC",70,100}, 
                        {"Electroweb","ELECTRIC",55,95}, {"Nuzzle","ELECTRIC",20,100}, {"Zing Zap","ELECTRIC",80,100}, 
                        {"Aura Wheel","ELECTRIC",110,100}, {"Overdrive","ELECTRIC",80,100}, {"Plasma Fists","ELECTRIC",100,100}, 
                        {"Bolt Strike","ELECTRIC",130,85}, {"Fusion Bolt","ELECTRIC",100,100},
                        {"Powder Snow","ICE",40,100}, {"Ice Beam","ICE",90,100}, {"Blizzard","ICE",110,70}, 
                        {"Ice Punch","ICE",75,100}, {"Aurora Beam","ICE",65,100}, {"Ice Shard","ICE",40,100}, 
                        {"Ice Fang","ICE",65,95}, {"Avalanche","ICE",60,100}, {"Icicle Crash","ICE",85,90}, 
                        {"Freeze-Dry","ICE",70,100}, {"Glaciate","ICE",65,95}, {"Frost Breath","ICE",60,90}, 
                        {"Ice Burn","ICE",140,90}, {"Icicle Spear","ICE",25,100}, {"Icy Wind","ICE",55,95}, 
                        {"Ice Hammer","ICE",100,90}, {"Triple Axel","ICE",20,90}, {"Mountain Gale","ICE",100,85}, 
                        {"Glacial Lance","ICE",120,100}, {"Subzero Slammer","ICE",175,100},
                        {"Poison Sting","POISON",15,100}, {"Sludge","POISON",65,100}, {"Sludge Bomb","POISON",90,100}, 
                        {"Sludge Wave","POISON",95,100}, {"Poison Jab","POISON",80,100}, {"Gunk Shot","POISON",120,80}, 
                        {"Poison Fang","POISON",50,100}, {"Cross Poison","POISON",70,100}, {"Clear Smog","POISON",50,100}, 
                        {"Venoshock","POISON",65,100}, {"Acid","POISON",40,100}, {"Acid Spray","POISON",40,100}, 
                        {"Belch","POISON",120,90}, {"Smog","POISON",30,70}, {"Poison Tail","POISON",50,100}, 
                        {"Gastro Acid","POISON",40,100}, {"Mortal Spin","POISON",30,100}, {"Shell Side Arm","POISON",90,100}, 
                        {"Dire Claw","POISON",80,100}, {"Venom Drench","POISON",60,100}
                    };
                    for (Object[] s : skillData) {
                        pstmtSkill.setString(1, (String) s[0]);
                        pstmtSkill.setInt(2, typeMap.get((String) s[1])); 
                        pstmtSkill.setInt(3, (int) s[2]);
                        pstmtSkill.setInt(4, (int) s[3]);
                        pstmtSkill.addBatch();
                    }
                    pstmtSkill.executeBatch();
                }
            }

        } catch (Exception e) {
            System.err.println("Gagal menginisialisasi database: " + e.getMessage());
            e.printStackTrace();
        }
    }
}