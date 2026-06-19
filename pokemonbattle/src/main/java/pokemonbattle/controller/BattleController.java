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

    // ========== 1. MULAI PERTARUNGAN (BOSS MODE) ==========
    @PostMapping("/start")
    public Map<String, Object> startBattle(@RequestBody StartRequest request, HttpSession session) {
        int playerPokemonId = request.getPlayerPokemonId();
        pokemonDAO.PokemonData player = pokemonDAO.getPokemonById(playerPokemonId);
        pokemonDAO.PokemonData enemy = pokemonDAO.getRandomPokemon();

        if (player == null || enemy == null) {
            return Map.of("error", "Pokémon tidak ditemukan");
        }

        // HP MUSUH DINAIKKAN MENJADI 4 KALI LIPAT (SUSAH/BOSS MODE)
        int enemyMaxHp = (int) (enemy.getMaxHp() * 4.0);
        int enemyCurrentHp = enemyMaxHp;

        List<Skill> enemyMoves = pokemonDAO.getRandomEnemyMoves(enemy.getType());
        List<Skill> playerMoves = pokemonDAO.getRandomEnemyMoves(player.getType());

        BattleState state = new BattleState(
                player.getId(), enemy.getId(),
                player.getHp(), player.getMaxHp(),
                enemyCurrentHp, enemyMaxHp
        );

        // Menyimpan data penting ke dalam HTTP Session
        session.setAttribute("battleState", state);
        session.setAttribute("enemyMoves", enemyMoves);
        session.setAttribute("playerMoves", playerMoves);
        session.setAttribute("pokemonLeft", 3); // Jatah 3 nyawa Pokemon mati
        session.setAttribute("swapLeft", 3);    // Jatah 3 kali ganti acak manual

        Map<String, Object> resp = new HashMap<>();
        resp.put("message", "Pertarungan dimulai! Musuh terlihat sangat kuat!");
        resp.put("player", pokemonToMap(player));
        resp.put("enemy", pokemonToMap(enemy));
        resp.put("enemyMaxHp", enemyMaxHp);
        resp.put("enemyCurrentHp", enemyCurrentHp);
        resp.put("playerMoves", playerMoves);
        resp.put("pokemonLeft", 3);
        resp.put("swapLeft", 3);
        resp.put("status", "active");

        return resp;
    }

    // ========== 2. GANTI POKEMON ACAK DI TENGAH BATTLE ==========
    @PostMapping("/swap-random")
    @SuppressWarnings("unchecked")
    public Map<String, Object> swapRandom(HttpSession session) {
        BattleState state = (BattleState) session.getAttribute("battleState");
        Integer swapLeft = (Integer) session.getAttribute("swapLeft");
        String username = (String) session.getAttribute("currentUser");

        if (state == null || swapLeft == null || username == null) {
            return Map.of("error", "Pertarungan tidak aktif atau sesi habis");
        }

        if (swapLeft <= 0) {
            return Map.of("error", "Jatah ganti Pokémon acak kamu sudah habis!");
        }

        String sql = "SELECT p.id, p.name, p.hp, p.max_hp, p.attack, p.defense, p.speed, p.rarity, t.name AS type_name "
                + "FROM user_pokemon up "
                + "JOIN pokemon p ON up.pokemon_id = p.id "
                + "JOIN types t ON p.type_id = t.id "
                + "JOIN users u ON up.user_id = u.id "
                + "WHERE u.username = ? AND p.id != ? "
                + "ORDER BY RANDOM() LIMIT 1";

        try (Connection conn = Database.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            pstmt.setInt(2, state.getPlayerPokemonId());

            try (ResultSet rs = pstmt.executeQuery()) {
                if (!rs.next()) {
                    return Map.of("error", "Kamu tidak memiliki Pokémon cadangan lain di koleksimu untuk ditukar!");
                }

                pokemonDAO.PokemonData nextPokemon = new pokemonDAO.PokemonData(
                        rs.getInt("id"), rs.getString("name"), rs.getInt("hp"),
                        rs.getInt("max_hp"), rs.getInt("attack"), rs.getInt("defense"),
                        rs.getInt("speed"), pokemonbattle.models.PokemonType.valueOf(rs.getString("type_name")),
                        rs.getString("rarity")
                );

                state.setPlayerPokemonId(nextPokemon.getId());
                state.setPlayerCurrentHp(nextPokemon.getHp());
                state.setPlayerMaxHp(nextPokemon.getMaxHp());

                swapLeft--;
                session.setAttribute("swapLeft", swapLeft);

                List<Skill> playerMoves = pokemonDAO.getRandomEnemyMoves(nextPokemon.getType());
                session.setAttribute("playerMoves", playerMoves);

                Map<String, Object> resp = new HashMap<>();
                resp.put("success", true);
                resp.put("message", "Kamu menggunakan jatah tukar! Keluar secara acak: " + nextPokemon.getName() + "!");
                resp.put("player", pokemonToMap(nextPokemon));
                resp.put("playerMoves", playerMoves);
                resp.put("swapLeft", swapLeft);

                pokemonDAO.PokemonData enemy = pokemonDAO.getPokemonById(state.getEnemyPokemonId());
                if (enemy != null) {
                    enemyTurn(state, session, enemy, resp);
                    resp.put("playerCurrentHp", state.getPlayerCurrentHp());
                    resp.put("playerMaxHp", state.getPlayerMaxHp());
                    resp.put("playerAlive", state.isPlayerAlive());

                    Integer pokemonLeft = (Integer) session.getAttribute("pokemonLeft");
                    if (!state.isPlayerAlive() && pokemonLeft != null && pokemonLeft <= 1) {
                        Map<String, Object> rewardData = updateStats(username, false);
                        resp.put("rewardMessage", rewardData.get("rewardMessage"));
                        session.removeAttribute("battleState");
                    }
                }
                return resp;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Map.of("error", "Gagal melakukan swap cadangan");
        }
    }

    // ========== 3. DEPLOY POKEMON BARU MANUAL (SAAT MATI) ==========
    @PostMapping("/deploy-next")
    public Map<String, Object> deployNext(@RequestBody StartRequest request, HttpSession session) {
        BattleState state = (BattleState) session.getAttribute("battleState");
        Integer pokemonLeft = (Integer) session.getAttribute("pokemonLeft");

        if (state == null || pokemonLeft == null) {
            return Map.of("error", "Tidak ada pertarungan aktif");
        }
        if (pokemonLeft <= 1) {
            return Map.of("error", "Kesempatan habis!");
        }

        int nextPokemonId = request.getPlayerPokemonId();
        pokemonDAO.PokemonData nextPokemon = pokemonDAO.getPokemonById(nextPokemonId);
        if (nextPokemon == null) {
            return Map.of("error", "Pokémon tidak ditemukan");
        }

        state.setPlayerPokemonId(nextPokemon.getId());
        state.setPlayerCurrentHp(nextPokemon.getHp());
        state.setPlayerMaxHp(nextPokemon.getMaxHp());

        pokemonLeft--;
        session.setAttribute("pokemonLeft", pokemonLeft);

        List<Skill> playerMoves = pokemonDAO.getRandomEnemyMoves(nextPokemon.getType());
        session.setAttribute("playerMoves", playerMoves);

        Map<String, Object> resp = new HashMap<>();
        resp.put("success", true);
        resp.put("message", nextPokemon.getName() + ", maju!");
        resp.put("player", pokemonToMap(nextPokemon));
        resp.put("playerMoves", playerMoves);
        resp.put("pokemonLeft", pokemonLeft);
        resp.put("enemyCurrentHp", state.getEnemyCurrentHp());
        resp.put("enemyMaxHp", state.getEnemyMaxHp());

        return resp;
    }

    // ========== 4. EKSEKUSI ATTACK TURNS ==========
    @PostMapping("/attack")
    @SuppressWarnings("unchecked")
    public Map<String, Object> attack(@RequestBody AttackRequest request, HttpSession session) {
        BattleState state = (BattleState) session.getAttribute("battleState");
        Integer pokemonLeft = (Integer) session.getAttribute("pokemonLeft");
        if (state == null || pokemonLeft == null) {
            return Map.of("error", "Tidak ada pertarungan aktif");
        }

        String username = (String) session.getAttribute("currentUser");
        if (username == null) {
            return Map.of("error", "Tidak login");
        }

        int skillSlot = request.getSkillSlot();
        List<Skill> playerMoves = (List<Skill>) session.getAttribute("playerMoves");
        if (playerMoves == null || playerMoves.size() < skillSlot) {
            return Map.of("error", "Data skill tidak sinkron");
        }

        Skill playerSkill = playerMoves.get(skillSlot - 1);
        pokemonDAO.PokemonData player = pokemonDAO.getPokemonById(state.getPlayerPokemonId());
        pokemonDAO.PokemonData enemy = pokemonDAO.getPokemonById(state.getEnemyPokemonId());

        Map<String, Object> resp = new HashMap<>();

        // Pengecekan Akurasi Meleset
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
            resp.put("pokemonLeft", pokemonLeft);

            if (!state.isPlayerAlive() && pokemonLeft <= 1) {
                Map<String, Object> rewardData = updateStats(username, false);
                resp.put("rewardMessage", rewardData.get("rewardMessage"));
                session.removeAttribute("battleState");
            }
            return resp;
        }

        // Kalkulasi Damage Masuk ke Musuh
        int damage = battleService.calculateDamage(
                playerSkill.getPower(), player.getAttack(), enemy.getDefense(),
                playerSkill.getType(), enemy.getType()
        );
        state.reduceEnemyHp(damage);

        Status inflicted = battleService.tryInflictStatus(playerSkill.getType());
        if (inflicted != null) {
            state.setEnemyStatus(inflicted);
        }

        resp.put("hit", true);
        resp.put("damage", damage);
        resp.put("effectiveness", battleService.getEffectivenessText(playerSkill.getType(), enemy.getType()));
        resp.put("inflictedStatus", inflicted != null ? inflicted.name() : null);
        resp.put("enemyCurrentHp", state.getEnemyCurrentHp());
        resp.put("enemyMaxHp", state.getEnemyMaxHp());

        // JIKA MUSUH MATI: Jangan hapus session dulu di sini agar tombol Catch bisa berjalan
        if (!state.isEnemyAlive()) {
            resp.put("enemyAlive", false);
            resp.put("message", "Musuh kalah!");
            resp.put("playerCurrentHp", state.getPlayerCurrentHp());
            resp.put("playerMaxHp", state.getPlayerMaxHp());
            resp.put("playerAlive", true);
            Map<String, Object> rewardData = updateStats(username, true);
            resp.put("rewardMessage", rewardData.get("rewardMessage"));
            return resp;
        }

        // Giliran Musuh Membalas Hantaman
        enemyTurn(state, session, enemy, resp);
        resp.put("playerCurrentHp", state.getPlayerCurrentHp());
        resp.put("playerMaxHp", state.getPlayerMaxHp());
        resp.put("enemyAlive", true);
        resp.put("playerAlive", state.isPlayerAlive());
        resp.put("pokemonLeft", pokemonLeft);

        // Jika Player Mati dan sisa nyawa tim habis total
        if (!state.isPlayerAlive() && pokemonLeft <= 1) {
            Map<String, Object> rewardData = updateStats(username, false);
            resp.put("rewardMessage", rewardData.get("rewardMessage"));
            session.removeAttribute("battleState");
        }
        return resp;
    }

    // ========== 5. MENGGUNAKAN ITEM (STOK BERKURANG & COMBAT REVIVE) ==========
    @PostMapping("/item")
    public Map<String, Object> useItem(@RequestBody ItemRequest request, HttpSession session) {
        BattleState state = (BattleState) session.getAttribute("battleState");
        Integer pokemonLeft = (Integer) session.getAttribute("pokemonLeft");
        if (state == null || pokemonLeft == null) {
            return Map.of("error", "Tidak ada pertarungan aktif");
        }

        String itemName = request.getItemName();
        String username = (String) session.getAttribute("currentUser");
        if (username == null) {
            return Map.of("error", "Tidak login");
        }

        Map<String, Object> resp = new HashMap<>();

        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false); // Transaksi Aman SQLite

            PreparedStatement psItem = conn.prepareStatement("SELECT id, effect_value, type FROM item WHERE name = ?");
            psItem.setString(1, itemName);
            ResultSet rsItem = psItem.executeQuery();
            if (!rsItem.next()) {
                return Map.of("error", "Item tidak ditemukan");
            }

            int itemId = rsItem.getInt("id");
            int effect = rsItem.getInt("effect_value");
            String type = rsItem.getString("type");

            PreparedStatement psInv = conn.prepareStatement(
                    "SELECT ui.quantity, ui.user_id FROM user_inventory ui JOIN users u ON ui.user_id = u.id WHERE u.username = ? AND ui.item_id = ?"
            );
            psInv.setString(1, username);
            psInv.setInt(2, itemId);
            ResultSet rsInv = psInv.executeQuery();
            if (!rsInv.next() || rsInv.getInt("quantity") <= 0) {
                conn.rollback();
                return Map.of("error", "Stok item " + itemName + " habis!");
            }

            int currentQty = rsInv.getInt("quantity");
            int userId = rsInv.getInt("user_id");

            // POTONG STOK DI DATABASE: Mengurangi jumlah item sebanyak 1
            PreparedStatement psUpdateInv = conn.prepareStatement(
                    "UPDATE user_inventory SET quantity = ? WHERE user_id = ? AND item_id = ?"
            );
            psUpdateInv.setInt(1, currentQty - 1);
            psUpdateInv.setInt(2, userId);
            psUpdateInv.setInt(3, itemId);
            psUpdateInv.executeUpdate();

            // Jalankan Logika Efek Item
            if (type.equals("HEAL")) {
                if (state.getPlayerCurrentHp() <= 0) {
                    conn.rollback();
                    return Map.of("error", "Pokémon sudah pingsan! Gunakan Revive.");
                }
                int newHp = Math.min(state.getPlayerCurrentHp() + effect, state.getPlayerMaxHp());
                state.setPlayerCurrentHp(newHp);
                resp.put("message", "Menggunakan " + itemName + ". Sisa: " + (currentQty - 1) + ". HP pulih +" + effect);
            } else if (type.equals("CURE")) {
                state.setPlayerStatus(Status.NONE);
                resp.put("message", "Menggunakan " + itemName + ". Sisa: " + (currentQty - 1) + ". Status negatif pulih!");
            } else if (type.equals("REVIVE")) {
                if (state.getPlayerCurrentHp() <= 0) {
                    state.setPlayerCurrentHp(state.getPlayerMaxHp() / 2); // Kebal bangkit 50% darah
                    resp.put("message", "Menggunakan Revive! Pokémon bangkit kembali dengan setengah HP!");
                } else {
                    conn.rollback();
                    return Map.of("error", "Pokémon kamu belum pingsan, Revive tidak bisa digunakan!");
                }
            }

            conn.commit(); // Eksekusi Transaksi Berhasil

            pokemonDAO.PokemonData enemy = pokemonDAO.getPokemonById(state.getEnemyPokemonId());
            if (enemy != null && state.isEnemyAlive() && state.isPlayerAlive()) {
                enemyTurn(state, session, enemy, resp);
                resp.put("playerCurrentHp", state.getPlayerCurrentHp());
                resp.put("playerMaxHp", state.getPlayerMaxHp());
                resp.put("enemyCurrentHp", state.getEnemyCurrentHp());
                resp.put("enemyMaxHp", state.getEnemyMaxHp());
                resp.put("enemyAlive", state.isEnemyAlive());
                resp.put("playerAlive", state.isPlayerAlive());
                resp.put("pokemonLeft", pokemonLeft);

                if (!state.isPlayerAlive() && pokemonLeft <= 1) {
                    Map<String, Object> rewardData = updateStats(username, false);
                    resp.put("rewardMessage", rewardData.get("rewardMessage"));
                    session.removeAttribute("battleState");
                }
            }
            return resp;
        } catch (Exception e) {
            e.printStackTrace();
            return Map.of("error", "Gagal memproses item");
        }
    }

    // ========== 6. FASE HADIAH MENANGKAP BOSS (MAKSIMAL 3 LEMPARAN) ==========
    @PostMapping("/catch")
    public Map<String, Object> catchPokemon(@RequestBody CatchRequest request, HttpSession session) {
        BattleState state = (BattleState) session.getAttribute("battleState");
        if (state == null) {
            return Map.of("error", "Tidak ada pertarungan aktif");
        }

        if (state.getEnemyCurrentHp() > 0) {
            return Map.of("error", "Kalahkan Pokémon musuh terlebih dahulu sebelum mencoba menangkapnya!");
        }

        String username = (String) session.getAttribute("currentUser");
        pokemonDAO.PokemonData enemy = pokemonDAO.getPokemonById(state.getEnemyPokemonId());
        if (enemy == null) {
            return Map.of("error", "Data musuh tidak ditemukan");
        }

        boolean success = battleService.isCatchSuccessful(request.getAttempt(), enemy.getRarity());
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
            session.removeAttribute("battleState"); // Sukses tertangkap -> hapus sesi bertarung
            resp.put("caught", true);
            resp.put("message", "Hebat! " + enemy.getName() + " berhasil masuk ke dalam Pokeball dan menjadi milikmu! 🎉");
        } else {
            resp.put("caught", false);
            resp.put("message", "Pokeball bergetar... Ah! " + enemy.getName() + " berhasil keluar dari Pokeball!");
            
            // Jatah 3 kali lempar habis total -> hapus sesi bertarung agar kabur
            if (request.getAttempt() >= 3) {
                session.removeAttribute("battleState");
            }
        }
        return resp;
    }

    // ========== 7. LOGIKA GILIRAN MUSUH (BOSS ATTACK) ==========
    @SuppressWarnings("unchecked")
    private void enemyTurn(BattleState state, HttpSession session, pokemonDAO.PokemonData enemy, Map<String, Object> resp) {
        if (!state.isEnemyAlive() || !state.isPlayerAlive()) {
            return;
        }

        // Efek negatif status menyerang HP bos musuh
        if (state.getEnemyStatus() != Status.NONE) {
            int statusDmg = battleService.getStatusDamage(state.getEnemyStatus(), state.getEnemyMaxHp());
            if (statusDmg > 0) {
                state.reduceEnemyHp(statusDmg);
                if (!state.isEnemyAlive()) {
                    resp.put("enemyAlive", false);
                    resp.put("message", "Musuh kalah karena efek status!");
                    String username = (String) session.getAttribute("currentUser");
                    if (username != null) {
                        Map<String, Object> rewardData = updateStats(username, true);
                        resp.put("rewardMessage", rewardData.get("rewardMessage"));
                    }
                    return; 
                }
            }
        }

        List<Skill> enemyMoves = (List<Skill>) session.getAttribute("enemyMoves");
        Skill enemySkill = enemyMoves.get(new Random().nextInt(enemyMoves.size()));
        pokemonDAO.PokemonData player = pokemonDAO.getPokemonById(state.getPlayerPokemonId());

        int rawDamage = battleService.calculateDamage(
                enemySkill.getPower(), enemy.getAttack(), player.getDefense(),
                enemySkill.getType(), player.getType()
        );

        int damage = (int) (rawDamage * 0.6); // Kalibrasi damage boss seimbang
        state.reducePlayerHp(damage);

        resp.put("enemyAttackMessage", "Musuh menggunakan " + enemySkill.getName() + "!");
        resp.put("enemyAttackDamage", damage);
    }

    // ========== 8. REWARD DROP SYSTEM PASCA-BATTLE ==========
    private Map<String, Object> updateStats(String username, boolean isWin) {
        String column = isWin ? "wins" : "losses";
        String sqlUpdateUser = "UPDATE users SET " + column + " = " + column + " + 1 WHERE username = ?";
        Map<String, Object> rewardResp = new HashMap<>();

        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);

            // 1. Update stats win/loss user
            try (PreparedStatement pstmt = conn.prepareStatement(sqlUpdateUser)) {
                pstmt.setString(1, username);
                pstmt.executeUpdate();
            }

            // 2. Kalkulasi hadiah database
            PreparedStatement psUser = conn.prepareStatement("SELECT id, wins, losses FROM users WHERE username = ?");
            psUser.setString(1, username);
            ResultSet rsUser = psUser.executeQuery();

            if (rsUser.next()) {
                int userId = rsUser.getInt("id");
                int totalMatches = rsUser.getInt("wins") + rsUser.getInt("losses");

                Random rand = new Random();
                String msg = "";

                if (isWin) {
                    // MENANG: Dapat 2-3 Potion / Antidote secara acak
                    String rewardName = rand.nextBoolean() ? "Potion" : "Antidote";
                    int qtyGot = rand.nextInt(2) + 2; 
                    giveItemReward(conn, userId, rewardName, qtyGot);
                    msg = "Mendapatkan hadiah kemenangan: " + qtyGot + "x " + rewardName + "!";
                } else {
                    // KALAH: Dapat hadiah hiburan pelipur lara (1 Mini Potion)
                    giveItemReward(conn, userId, "Mini Potion", 1);
                    msg = "Mendapatkan hadiah hiburan kalah: 1x Mini Potion!";
                }

                // EKSKLUSIF KELIPATAN 5 PERTANDINGAN -> DIHADIAHI 1 REVIVE!
                if (totalMatches % 5 == 0) {
                    giveItemReward(conn, userId, "Revive", 1);
                    msg += " 🎉 BONUS Kelipatan 5 Match: Mendapatkan 1x Revive!";
                }

                rewardResp.put("rewardMessage", msg);
            }
            conn.commit();
        } catch (Exception e) {
            e.printStackTrace();
            rewardResp.put("rewardMessage", "Gagal memproses klaim hadiah database.");
        }
        return rewardResp;
    }

    private void giveItemReward(Connection conn, int userId, String itemName, int quantity) throws Exception {
        PreparedStatement psGetItem = conn.prepareStatement("SELECT id FROM item WHERE name = ?");
        psGetItem.setString(1, itemName);
        try (ResultSet rsItem = psGetItem.executeQuery()) {
            if (rsItem.next()) {
                int itemId = rsItem.getInt("id");
                String sqlGive = "INSERT INTO user_inventory (user_id, item_id, quantity) VALUES (?, ?, ?) "
                        + "ON CONFLICT(user_id, item_id) DO UPDATE SET quantity = quantity + ?";
                try (PreparedStatement psGive = conn.prepareStatement(sqlGive)) {
                    psGive.setInt(1, userId);
                    psGive.setInt(2, itemId);
                    psGive.setInt(3, quantity);
                    psGive.setInt(4, quantity);
                    psGive.executeUpdate();
                }
            }
        }
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

    // ========== DTO REQUEST BODIES STRUCTURES ==========
    static class StartRequest {
        private int playerPokemonId;
        public int getPlayerPokemonId() { return playerPokemonId; }
        public void setPlayerPokemonId(int playerPokemonId) { this.playerPokemonId = playerPokemonId; }
    }

    static class AttackRequest {
        private int skillSlot;
        public int getSkillSlot() { return skillSlot; }
    }

    static class ItemRequest {
        private String itemName;
        public String getItemName() { return itemName; }
    }

    static class CatchRequest {
        private int attempt;
        public int getAttempt() { return attempt; }
        public void setAttempt(int attempt) { this.attempt = attempt; }
    }
}