package pokemonbattle.service;

import pokemonbattle.models.PokemonType;
import pokemonbattle.models.Status;
import java.util.*;

import org.springframework.stereotype.Service;

@Service
public class BattleService {

    private final Random random = new Random();

    // ==================== TYPE CHART ====================
    private static final Map<PokemonType, Set<PokemonType>> effective = new HashMap<>();
    private static final Map<PokemonType, Set<PokemonType>> notEffective = new HashMap<>();

    static {
        effective.put(PokemonType.FIRE, Set.of(PokemonType.GRASS, PokemonType.ICE));
        notEffective.put(PokemonType.FIRE, Set.of(PokemonType.FIRE, PokemonType.WATER));

        effective.put(PokemonType.WATER, Set.of(PokemonType.FIRE));
        notEffective.put(PokemonType.WATER, Set.of(PokemonType.WATER, PokemonType.GRASS));

        effective.put(PokemonType.GRASS, Set.of(PokemonType.WATER));
        notEffective.put(PokemonType.GRASS, Set.of(PokemonType.FIRE, PokemonType.GRASS, PokemonType.POISON));

        effective.put(PokemonType.ELECTRIC, Set.of(PokemonType.WATER));
        notEffective.put(PokemonType.ELECTRIC, Set.of(PokemonType.ELECTRIC, PokemonType.GRASS));

        effective.put(PokemonType.ICE, Set.of(PokemonType.GRASS));
        notEffective.put(PokemonType.ICE, Set.of(PokemonType.FIRE, PokemonType.WATER, PokemonType.ICE));

        effective.put(PokemonType.POISON, Set.of(PokemonType.GRASS));
        notEffective.put(PokemonType.POISON, Set.of(PokemonType.POISON));
    }

    // ==================== STATUS EFFECT MAPPING ====================
    private static final Map<PokemonType, Status> statusMap = new HashMap<>();
    static {
        statusMap.put(PokemonType.FIRE, Status.BURN);
        statusMap.put(PokemonType.POISON, Status.POISON);
        statusMap.put(PokemonType.ELECTRIC, Status.PARALYSIS);
        statusMap.put(PokemonType.ICE, Status.FREEZE);
        statusMap.put(PokemonType.WATER, Status.SLEEP);
        statusMap.put(PokemonType.GRASS, Status.SLEEP);
        // NORMAL tidak masuk → null
    }

    /**
     * Mengembalikan status yang mungkin dikenakan oleh tipe serangan tertentu.
     * @param attackType tipe serangan
     * @return Status atau null jika tipe tidak memberikan status
     */
    public Status getStatusForType(PokemonType attackType) {
        return statusMap.get(attackType);
    }

    /**
     * Mencoba memberikan status efek setelah serangan kena.
     * @param attackType tipe serangan yang digunakan
     * @return Status jika berhasil (chance 15%), null jika gagal
     */
    public Status tryInflictStatus(PokemonType attackType) {
        Status possible = statusMap.get(attackType);
        if (possible != null && random.nextDouble() < 0.15) {
            return possible;
        }
        return null;
    }

    // ==================== DAMAGE ====================
    public double getTypeMultiplier(PokemonType attackType, PokemonType defenseType) {
        if (effective.getOrDefault(attackType, Set.of()).contains(defenseType)) {
            return 1.5;
        }
        if (notEffective.getOrDefault(attackType, Set.of()).contains(defenseType)) {
            return 0.75;
        }
        return 1.0;
    }

    public int calculateDamage(int power, int attackStat, int defenseStat,
                               PokemonType attackType, PokemonType defenseType) {
        double typeMult = getTypeMultiplier(attackType, defenseType);
        double base = power * ((double) attackStat / defenseStat);
        double randomFactor = 0.85 + (random.nextDouble() * 0.15);
        double total = base * typeMult * randomFactor;
        return Math.max(1, (int) total);
    }

    public boolean isHit(int accuracy) {
        return random.nextInt(100) < accuracy;
    }

    public String getEffectivenessText(PokemonType attackType, PokemonType defenseType) {
        double mult = getTypeMultiplier(attackType, defenseType);
        if (mult > 1.0) return "effective";
        if (mult < 1.0) return "not_effective";
        return "normal";
    }

    /**
     * Menghitung apakah Pokémon liar berhasil ditangkap.
     * Rumus sederhana: makin sedikit HP, makin mudah.
     * chance = (1 - currentHp/maxHp) * 0.5 + 0.1  (min 0.1, max 0.6)
     */
    public boolean isCatchSuccessful(int currentHp, int maxHp) {
        double hpFactor = 1.0 - ((double) currentHp / maxHp);
        double chance = hpFactor * 0.5 + 0.1;
        return random.nextDouble() < chance;
    }

    /**
     * Mendapatkan damage yang diterima akibat status (per turn).
     * Hanya BURN dan POISON yang memberi damage.
     */
    public int getStatusDamage(Status status, int maxHp) {
        if (status == Status.BURN || status == Status.POISON) {
            return Math.max(1, maxHp / 16); // 1/16 HP seperti di Pokémon
        }
        return 0;
    }

    /**
     * Cek apakah Pokémon bisa bergerak (tidak freeze/sleep/paralyzed)
     */
    public boolean canMove(Status status) {
        if (status == Status.FREEZE || status == Status.SLEEP) {
            return false; // tidak bisa bergerak sama sekali
        }
        if (status == Status.PARALYSIS) {
            return random.nextDouble() < 0.75; // 25% chance gagal
        }
        return true;
    }
}