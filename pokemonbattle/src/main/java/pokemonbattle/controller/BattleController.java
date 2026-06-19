package pokemonbattle.controller;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpSession;
import pokemonbattle.database.Database;
import pokemonbattle.database.pokemonDAO;
import pokemonbattle.models.BattleService;
import pokemonbattle.models.BattleState;
import pokemonbattle.models.Skill;
import pokemonbattle.models.Status;

@RestController
@RequestMapping("/api/battle")
public class BattleController {

    @Autowired
    private BattleService battleService;

    // ========== MULAI PERTARUNGAN ==========
    @PostMapping("/start")
    public Map<String, Object> startBattle(@RequestBody StartRequest request, HttpSession session) {
        int playerPokemonId = request.getPlayerPokemonId();
        pokemonDAO.PokemonData player = pokemonDAO.getPokemonById(playerPokemonId);
        pokemonDAO.PokemonData enemy = pokemonDAO.getRandomPokemon();
        if (player == null || enemy == null) return Map.of("error", "Pokémon tidak ditemukan");

        int enemyMaxHp = (int) (enemy.getMaxHp() * 2.5);
        int enemyCurrentHp = enemyMaxHp;
        List<Skill> enemyMoves = pokemonDAO.getRandomEnemyMoves(enemy.getType());

        BattleState state = new BattleState(
            player.getId(), enemy.getId(),
            player.getHp(), player.getMaxHp(),
            enemyCurrentHp, enemyMaxHp
        );
        session.setAttribute("battleState", state);
        session.setAttribute("enemyMoves", enemyMoves);

        Map<String, Object> resp = new HashMap<>();
        resp.put("message", "Pertarungan dimulai!");
        resp.put("player", pokemonToMap(player));
        resp.put("enemy", pokemonToMap(enemy));
        resp.put("enemyMaxHp", enemyMaxHp);
        resp.put("enemyCurrentHp", enemyCurrentHp);
        resp.put("enemyMoves", enemyMoves);
        resp.put("status", "active");
        return resp;
    }

    // ========== SERANG ==========
    @PostMapping("/attack")
    public Map<String, Object> attack(@RequestBody AttackRequest request, HttpSession session) {
        BattleState state = (BattleState) session.getAttribute("battleState");
        if (state == null) return Map.of("error", "Tidak ada pertarungan aktif");

        String username = (String) session.getAttribute("currentUser");
        if (username == null) return Map.of("error", "Tidak login");

        int skillSlot = request.getSkillSlot();
        if (skillSlot < 1 || skillSlot > 4) return Map.of("error", "Skill tidak valid");

        Skill playerSkill = getPlayerSkill(state.getPlayerPokemonId(), skillSlot);
        pokemonDAO.PokemonData player = pokemonDAO.getPokemonById(state.getPlayerPokemonId());
        pokemonDAO.PokemonData enemy = pokemonDAO.getPokemonById(state.getEnemyPokemonId());

        if (playerSkill == null || player == null || enemy == null)
            return Map.of("error", "Data tidak lengkap");

        Map<String, Object> resp = new HashMap<>();

        // Cek akurasi
        if (!battleService.isHit(playerSkill.getAccuracy())) {
            resp.put("hit", false);
            resp.put("message", "Serangan meleset!");
            enemyTurn(state, session, enemy, resp);
            resp.put("enemyCurrentHp", state.getEnemyCurrentHp());
            resp.put("enemyMaxHp", state.getEnemyMaxHp());
            resp.put("playerCurrentHp", state.getPlayerCurrentHp());
            resp.put("playerMaxHp", state.getPlayerMaxHp());
            resp.put("enemyAlive", state.isEnemyAlive());
            resp.put("playerAlive", state.isPlayerAlive());

            if (!state.isPlayerAlive()) {
                updateStats(username, false);
                session.removeAttribute("battleState");
            }
            return resp;
        }

        // Hitung damage
        int damage = battleService.calculateDamage(
            playerSkill.getPower(), player.getAttack(), enemy.getDefense(),
            playerSkill.getType(), enemy.getType()
        );
        state.reduceEnemyHp(damage);

        Status inflicted = battleService.tryInflictStatus(playerSkill.getType());
        if (inflicted != null) state.setEnemyStatus(inflicted);

        resp.put("hit", true);
        resp.put("damage", damage);
        resp.put("effectiveness", battleService.getEffectivenessText(playerSkill.getType(), enemy.getType()));
        resp.put("inflictedStatus", inflicted != null ? inflicted.name() : null);
        resp.put("enemyCurrentHp", state.getEnemyCurrentHp());
        resp.put("enemyMaxHp", state.getEnemyMaxHp());

        if (!state.isEnemyAlive()) {
            resp.put("enemyAlive", false);
            resp.put("message", "Musuh kalah!");
            resp.put("playerCurrentHp", state.getPlayerCurrentHp());
            resp.put("playerMaxHp", state.getPlayerMaxHp());
            resp.put("playerAlive", true);
            updateStats(username, true);
            session.removeAttribute("battleState");
            return resp;
        }

        // Giliran musuh
        enemyTurn(state, session, enemy, resp);
        resp.put("playerCurrentHp", state.getPlayerCurrentHp());
        resp.put("playerMaxHp", state.getPlayerMaxHp());
        resp.put("enemyAlive", true);
        resp.put("playerAlive", state.isPlayerAlive());

        if (!state.isPlayerAlive()) {
            updateStats(username, false);
            session.removeAttribute("battleState");
        }
        return resp;
    }

    // ========== MENGGUNAKAN ITEM ==========
    @PostMapping("/item")
    public Map<String, Object> useItem(@RequestBody ItemRequest request, HttpSession session) {
        BattleState state = (BattleState) session.getAttribute("battleState");
        if (state == null) return Map.of("error", "Tidak ada pertarungan aktif");

        String itemName = request.getItemName();
        String username = (String) session.getAttribute("currentUser");
        if (username == null) return Map.of("error", "Tidak login");

        Map<String, Object> resp = new HashMap<>();

        try (Connection conn = Database.getConnection()) {
            PreparedStatement psItem = conn.prepareStatement("SELECT id, effect_value, type FROM item WHERE name = ?");
            psItem.setString(1, itemName);
            ResultSet rsItem = psItem.executeQuery();
            if (!rsItem.next()) return Map.of("error", "Item tidak ditemukan");

            int itemId = rsItem.getInt("id");
            int effect = rsItem.getInt("effect_value");
            String type = rsItem.getString("type");

            // Cek ketersediaan item (tapi tidak akan dikurangi)
            PreparedStatement psInv = conn.prepareStatement(
                "SELECT ui.quantity FROM user_inventory ui JOIN users u ON ui.user_id = u.id WHERE u.username = ? AND ui.item_id = ?"
            );
            psInv.setString(1, username);
            psInv.setInt(2, itemId);
            ResultSet rsInv = psInv.executeQuery();
            if (!rsInv.next() || rsInv.getInt(1) <= 0) {
                return Map.of("error", "Item tidak tersedia");
            }

            // ===== BAGIAN PENGURANGAN STOK DIHILANGKAN =====
            // Tidak jadi update database agar item tidak hilang permanen
            // nanti setelah battle selesai, stok tetap utuh

            // Terapkan efek item
            if (type.equals("HEAL")) {
                int newHp = Math.min(state.getPlayerCurrentHp() + effect, state.getPlayerMaxHp());
                state.setPlayerCurrentHp(newHp);
                resp.put("message", "Menggunakan " + itemName + " memulihkan " + effect + " HP!");
                resp.put("playerCurrentHp", newHp);
                resp.put("playerMaxHp", state.getPlayerMaxHp());
            } else if (type.equals("CURE")) {
                state.setPlayerStatus(Status.NONE);
                resp.put("message", "Menggunakan " + itemName + " menyembuhkan status!");
                resp.put("playerStatus", "NONE");
            } else {
                resp.put("message", "Item " + itemName + " digunakan, tetapi tidak ada efek langsung.");
            }

            // Setelah item digunakan, musuh langsung menyerang balik
            pokemonDAO.PokemonData enemy = pokemonDAO.getPokemonById(state.getEnemyPokemonId());
            if (enemy != null && state.isEnemyAlive() && state.isPlayerAlive()) {
                enemyTurn(state, session, enemy, resp);
                resp.put("playerCurrentHp", state.getPlayerCurrentHp());
                resp.put("playerMaxHp", state.getPlayerMaxHp());
                resp.put("enemyCurrentHp", state.getEnemyCurrentHp());
                resp.put("enemyMaxHp", state.getEnemyMaxHp());
                resp.put("enemyAlive", state.isEnemyAlive());
                resp.put("playerAlive", state.isPlayerAlive());
                if (!state.isPlayerAlive()) {
                    updateStats(username, false);
                    session.removeAttribute("battleState");
                }
            }
            return resp;
        } catch (Exception e) {
            e.printStackTrace();
            return Map.of("error", "Gagal menggunakan item");
        }
    }

    // ========== MENCOBA MENANGKAP ==========
    @PostMapping("/catch")
    public Map<String, Object> catchPokemon(HttpSession session) {
        BattleState state = (BattleState) session.getAttribute("battleState");
        if (state == null) return Map.of("error", "Tidak ada pertarungan aktif");

        String username = (String) session.getAttribute("currentUser");
        boolean success = battleService.isCatchSuccessful(state.getEnemyCurrentHp(), state.getEnemyMaxHp());
        Map<String, Object> resp = new HashMap<>();

        if (success) {
            if (username != null) {
                try (Connection conn = Database.getConnection()) {
                    PreparedStatement psUser = conn.prepareStatement("SELECT id FROM users WHERE username = ?");
                    psUser.setString(1, username);
                    ResultSet rsUser = psUser.executeQuery();
                    if (rsUser.next()) {
                        int userId = rsUser.getInt(1);
                        PreparedStatement psInsert = conn.prepareStatement("INSERT INTO user_pokemon (user_id, pokemon_id) VALUES (?, ?)");
                        psInsert.setInt(1, userId);
                        psInsert.setInt(2, state.getEnemyPokemonId());
                        psInsert.executeUpdate();
                    }
                } catch (Exception e) { e.printStackTrace(); }
            }
            updateStats(username, true);
            session.removeAttribute("battleState");
            resp.put("caught", true);
            resp.put("message", "Berhasil menangkap Pokémon liar!");
            resp.put("battleEnd", true);
        } else {
            resp.put("caught", false);
            resp.put("message", "Gagal menangkap! Pokémon liar menyerang balik.");
            pokemonDAO.PokemonData enemy = pokemonDAO.getPokemonById(state.getEnemyPokemonId());
            if (enemy != null) {
                enemyTurn(state, session, enemy, resp);
                resp.put("playerCurrentHp", state.getPlayerCurrentHp());
                resp.put("playerMaxHp", state.getPlayerMaxHp());
                resp.put("enemyCurrentHp", state.getEnemyCurrentHp());
                resp.put("enemyMaxHp", state.getEnemyMaxHp());
                resp.put("enemyAlive", state.isEnemyAlive());
                resp.put("playerAlive", state.isPlayerAlive());
                if (!state.isPlayerAlive()) {
                    updateStats(username, false);
                    session.removeAttribute("battleState");
                }
            }
        }
        return resp;
    }

    // ========== GILIRAN MUSUH ==========
    @SuppressWarnings("unchecked")
    private void enemyTurn(BattleState state, HttpSession session, pokemonDAO.PokemonData enemy, Map<String, Object> resp) {
        if (!state.isEnemyAlive() || !state.isPlayerAlive()) return;

        // Status damage musuh
        if (state.getEnemyStatus() != Status.NONE) {
            int statusDmg = battleService.getStatusDamage(state.getEnemyStatus(), state.getEnemyMaxHp());
            if (statusDmg > 0) {
                state.reduceEnemyHp(statusDmg);
                resp.put("enemyCurrentHp", state.getEnemyCurrentHp());
                if (!state.isEnemyAlive()) {
                    resp.put("enemyAlive", false);
                    resp.put("message", "Musuh kalah karena efek status!");
                    session.removeAttribute("battleState");
                    return;
                }
            }
        }

        // Cek apakah musuh bisa bergerak
        if (state.getEnemyStatus() != Status.NONE && !battleService.canMove(state.getEnemyStatus())) {
            resp.put("enemyAttackMessage", "Musuh tidak bisa bergerak karena " + state.getEnemyStatus() + "!");
            return;
        }

        List<Skill> enemyMoves = (List<Skill>) session.getAttribute("enemyMoves");
        if (enemyMoves == null || enemyMoves.isEmpty()) {
            enemyMoves = pokemonDAO.getRandomEnemyMoves(enemy.getType());
            session.setAttribute("enemyMoves", enemyMoves);
        }
        Skill enemySkill = enemyMoves.get(new Random().nextInt(enemyMoves.size()));

        pokemonDAO.PokemonData player = pokemonDAO.getPokemonById(state.getPlayerPokemonId());
        if (player == null) return;

        int rawDamage = battleService.calculateDamage(
            enemySkill.getPower(), enemy.getAttack(), player.getDefense(),
            enemySkill.getType(), player.getType()
        );
        // Damage musuh dikurangi setengah agar tidak overpower
        int damage = (int) (rawDamage * 0.3);
        state.reducePlayerHp(damage);

        resp.put("enemyAttackMessage", "Musuh menggunakan " + enemySkill.getName() + "!");
        resp.put("enemyAttackDamage", damage);
        resp.put("playerCurrentHp", state.getPlayerCurrentHp());
        resp.put("playerMaxHp", state.getPlayerMaxHp());

        if (!state.isPlayerAlive()) {
            resp.put("playerAlive", false);
            resp.put("message", "Pokémonmu kalah...");
            // session.removeAttribute("battleState") akan dipanggil di pemanggil
        }
    }

    // ========== UPDATE STATISTIK ==========
    private void updateStats(String username, boolean isWin) {
        String column = isWin ? "wins" : "losses";
        String sql = "UPDATE users SET " + column + " = " + column + " + 1 WHERE username = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ========== HELPER ==========
    private Skill getPlayerSkill(int playerPokemonId, int slot) {
        pokemonDAO.PokemonData player = pokemonDAO.getPokemonById(playerPokemonId);
        if (player == null) return null;
        List<Skill> moves = pokemonDAO.getRandomEnemyMoves(player.getType());
        return (moves.size() >= slot) ? moves.get(slot - 1) : null;
    }

    private Map<String, Object> pokemonToMap(pokemonDAO.PokemonData p) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", p.getId());
        map.put("name", p.getName());
        map.put("hp", p.getHp());
        map.put("maxHp", p.getMaxHp());
        map.put("attack", p.getAttack());
        map.put("defense", p.getDefense());
        map.put("speed", p.getSpeed());
        map.put("type", p.getType().name());
        return map;
    }

    // ========== DTO ==========
    static class StartRequest {
        private int playerPokemonId;
        public int getPlayerPokemonId() { return playerPokemonId; }
        public void setPlayerPokemonId(int playerPokemonId) { this.playerPokemonId = playerPokemonId; }
    }

    static class AttackRequest {
        private int skillSlot;
        public int getSkillSlot() { return skillSlot; }
        public void setSkillSlot(int skillSlot) { this.skillSlot = skillSlot; }
    }

    static class ItemRequest {
        private String itemName;
        public String getItemName() { return itemName; }
        public void setItemName(String itemName) { this.itemName = itemName; }
    }
}