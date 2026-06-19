package pokemonbattle.controller;

import pokemonbattle.database.PokemonDAO;
import pokemonbattle.models.*;
import pokemonbattle.service.BattleService;

import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.http.HttpSession;
import java.util.*;

@RestController
@RequestMapping("/api/battle")
public class BattleController {

    @Autowired
    private BattleService battleService;

    // ========== MULAI PERTARUNGAN ==========
    @PostMapping("/start")
    public Map<String, Object> startBattle(@RequestBody StartRequest request, HttpSession session) {
        int playerPokemonId = request.getPlayerPokemonId();

        // Ambil data player & musuh
        PokemonDAO.PokemonData player = PokemonDAO.getPokemonById(playerPokemonId);
        PokemonDAO.PokemonData enemy = PokemonDAO.getRandomPokemon();

        if (player == null || enemy == null) {
            return Map.of("error", "Pokémon tidak ditemukan");
        }

        // Hitung HP musuh (2.5x)
        int enemyMaxHp = (int) (enemy.getMaxHp() * 2.5);
        int enemyCurrentHp = enemyMaxHp;

        // Ambil moveset musuh (4 skill: 1 Normal + 3 tipe musuh)
        List<Skill> enemyMoves = PokemonDAO.getRandomEnemyMoves(enemy.getType());

        // Simpan state di session
        BattleState state = new BattleState(
            player.getId(),
            enemy.getId(),
            player.getHp(),
            player.getMaxHp(),
            enemyCurrentHp,
            enemyMaxHp
        );
        session.setAttribute("battleState", state);

        // Response ke frontend
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
        if (state == null) {
            return Map.of("error", "Tidak ada pertarungan aktif");
        }

        int skillSlot = request.getSkillSlot(); // 1-4
        if (skillSlot < 1 || skillSlot > 4) {
            return Map.of("error", "Skill tidak valid");
        }

        // Ambil skill pemain (hardcode dulu, nanti bisa dari database)
        // Asumsi: kita butuh data skill pemain. Untuk sementara, kita ambil skill dari ID yang dikirim
        // atau buat DAO. Saya akan gunakan dummy skill jika belum ada.
        Skill playerSkill = getPlayerSkill(state.getPlayerPokemonId(), skillSlot); // implementasi di bawah

        // Ambil data Pokémon
        PokemonDAO.PokemonData player = PokemonDAO.getPokemonById(state.getPlayerPokemonId());
        PokemonDAO.PokemonData enemy = PokemonDAO.getPokemonById(state.getEnemyPokemonId());

        if (playerSkill == null || player == null || enemy == null) {
            return Map.of("error", "Data tidak lengkap");
        }

        // Cek akurasi
        if (!battleService.isHit(playerSkill.getAccuracy())) {
            Map<String, Object> resp = new HashMap<>();
            resp.put("hit", false);
            resp.put("message", "Serangan meleset!");
            // Giliran musuh tetap jalan? Sesuai desain, mungkin musuh balas.
            enemyTurn(state, session, enemy);
            return resp;
        }

        // Hitung damage
        int damage = battleService.calculateDamage(
            playerSkill.getPower(),
            player.getAttack(),
            enemy.getDefense(),
            playerSkill.getType(),
            enemy.getType()
        );

        // Kurangi HP musuh
        state.reduceEnemyHp(damage);

        // Cek status efek
        Status inflicted = battleService.tryInflictStatus(playerSkill.getType());
        if (inflicted != null) {
            state.setEnemyStatus(inflicted);
        }

        // Efektivitas
        String effectiveness = battleService.getEffectivenessText(playerSkill.getType(), enemy.getType());

        Map<String, Object> resp = new HashMap<>();
        resp.put("hit", true);
        resp.put("damage", damage);
        resp.put("enemyCurrentHp", state.getEnemyCurrentHp());
        resp.put("effectiveness", effectiveness);
        resp.put("inflictedStatus", inflicted != null ? inflicted.name() : null);
        resp.put("enemyAlive", state.isEnemyAlive());

        if (!state.isEnemyAlive()) {
            resp.put("message", "Musuh kalah!");
            session.removeAttribute("battleState");
        } else {
            // Giliran musuh
            enemyTurn(state, session, enemy);
            resp.put("playerCurrentHp", state.getPlayerCurrentHp());
            resp.put("playerAlive", state.isPlayerAlive());
        }

        return resp;
    }

    // ========== GILIRAN MUSUH SEDERHANA ==========
    private void enemyTurn(BattleState state, HttpSession session, PokemonDAO.PokemonData enemy) {
        if (!state.isEnemyAlive() || !state.isPlayerAlive()) return;

        // Ambil skill random dari enemy moves (simpan di session)
        @SuppressWarnings("unchecked")
        List<Skill> enemyMoves = (List<Skill>) session.getAttribute("enemyMoves");
        if (enemyMoves == null || enemyMoves.isEmpty()) {
            // fallback: ambil lagi
            enemyMoves = PokemonDAO.getRandomEnemyMoves(enemy.getType());
            session.setAttribute("enemyMoves", enemyMoves);
        }
        Skill enemySkill = enemyMoves.get(new Random().nextInt(enemyMoves.size()));

        // Kalkulasi damage (player = attacker? No, enemy attacks player)
        PokemonDAO.PokemonData player = PokemonDAO.getPokemonById(state.getPlayerPokemonId());
        if (player == null) return;

        int damage = battleService.calculateDamage(
            enemySkill.getPower(),
            enemy.getAttack(),
            player.getDefense(),
            enemySkill.getType(),
            player.getType()
        );

        state.reducePlayerHp(damage);
        if (!state.isPlayerAlive()) {
            session.removeAttribute("battleState");
        }
    }

    // ========== HELPER ==========
    private Map<String, Object> pokemonToMap(PokemonDAO.PokemonData p) {
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

    // Dummy skill player (nanti disesuaikan dengan mekanik sebenarnya)
    private Skill getPlayerSkill(int playerPokemonId, int slot) {
        // Sementara, ambil 4 skill acak sesuai tipe Pokémon pemain
        PokemonDAO.PokemonData player = PokemonDAO.getPokemonById(playerPokemonId);
        if (player == null) return null;
        // Ambil moveset sementara (seperti musuh)
        List<Skill> moves = PokemonDAO.getRandomEnemyMoves(player.getType());
        if (moves.size() >= slot) {
            return moves.get(slot - 1);
        }
        return null;
    }

    // ========== DTO CLASSES ==========
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
}