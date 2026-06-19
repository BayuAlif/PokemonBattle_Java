package pokemonbattle.database;

import pokemonbattle.models.PokemonType;
import pokemonbattle.models.Skill;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PokemonDAO {

    // Mengambil Pokémon berdasarkan ID (join ke types untuk ambil nama tipe)
    public static PokemonData getPokemonById(int pokemonId) {
        String sql = "SELECT p.id, p.name, p.hp, p.max_hp, p.attack, p.defense, p.speed, t.name AS type_name " +
                     "FROM pokemon p JOIN types t ON p.type_id = t.id WHERE p.id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, pokemonId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return new PokemonData(
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getInt("hp"),
                    rs.getInt("max_hp"),
                    rs.getInt("attack"),
                    rs.getInt("defense"),
                    rs.getInt("speed"),
                    PokemonType.valueOf(rs.getString("type_name"))
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // Mengambil Pokémon acak untuk musuh
    public static PokemonData getRandomPokemon() {
        String sql = "SELECT p.id, p.name, p.hp, p.max_hp, p.attack, p.defense, p.speed, t.name AS type_name " +
                     "FROM pokemon p JOIN types t ON p.type_id = t.id ORDER BY RANDOM() LIMIT 1";
        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery(sql);
            if (rs.next()) {
                return new PokemonData(
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getInt("hp"),
                    rs.getInt("max_hp"),
                    rs.getInt("attack"),
                    rs.getInt("defense"),
                    rs.getInt("speed"),
                    PokemonType.valueOf(rs.getString("type_name"))
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // Mengambil 4 skill acak untuk musuh: 1 NORMAL + 3 elemen acak
    public static List<Skill> getRandomEnemyMoves(PokemonType enemyType) {
        List<Skill> moves = new ArrayList<>();

        String normalSql = "SELECT id, name, type_id, power, accuracy FROM skill " +
                           "JOIN types ON skill.type_id = types.id " +
                           "WHERE types.name = 'NORMAL' ORDER BY RANDOM() LIMIT 1";
        String elementSql = "SELECT id, name, type_id, power, accuracy FROM skill " +
                            "JOIN types ON skill.type_id = types.id " +
                            "WHERE types.name = ? ORDER BY RANDOM() LIMIT 3";

        try (Connection conn = Database.getConnection()) {
            // 1 Normal
            try (Statement stmt = conn.createStatement()) {
                ResultSet rs = stmt.executeQuery(normalSql);
                if (rs.next()) {
                    moves.add(new Skill(rs.getInt("id"), rs.getString("name"),
                                        PokemonType.NORMAL,
                                        rs.getInt("power"), rs.getInt("accuracy")));
                }
            }
            // 3 Elemen (sesuai tipe musuh)
            try (PreparedStatement pstmt = conn.prepareStatement(elementSql)) {
                pstmt.setString(1, enemyType.name());
                ResultSet rs = pstmt.executeQuery();
                while (rs.next() && moves.size() < 4) {
                    moves.add(new Skill(rs.getInt("id"), rs.getString("name"),
                                        enemyType,
                                        rs.getInt("power"), rs.getInt("accuracy")));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return moves;
    }

    // Data sederhana Pokémon (tanpa objek penuh)
    public static class PokemonData {
        private int id;
        private String name;
        private int hp, maxHp, attack, defense, speed;
        private PokemonType type;

        public PokemonData(int id, String name, int hp, int maxHp, int attack, int defense, int speed, PokemonType type) {
            this.id = id;
            this.name = name;
            this.hp = hp;
            this.maxHp = maxHp;
            this.attack = attack;
            this.defense = defense;
            this.speed = speed;
            this.type = type;
        }

        public int getId() { return id; }
        public String getName() { return name; }
        public int getHp() { return hp; }
        public int getMaxHp() { return maxHp; }
        public int getAttack() { return attack; }
        public int getDefense() { return defense; }
        public int getSpeed() { return speed; }
        public PokemonType getType() { return type; }
    }
}