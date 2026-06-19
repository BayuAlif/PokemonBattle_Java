package pokemonbattle.models;

import java.io.Serializable;

public class BattleState implements Serializable {
    
    // Pokémon yang terlibat (ID)
    private int playerPokemonId;
    private int enemyPokemonId;

    // HP Player
    private int playerCurrentHp;
    private int playerMaxHp;

    // HP Musuh
    private int enemyCurrentHp;
    private int enemyMaxHp;

    // Status efek (NONE, BURN, POISON, PARALYSIS, FREEZE, SLEEP)
    private Status playerStatus = Status.NONE;
    private Status enemyStatus = Status.NONE;

    // ========== CONSTRUCTOR ==========
    public BattleState() {}

    public BattleState(int playerPokemonId, int enemyPokemonId,
                       int playerCurrentHp, int playerMaxHp,
                       int enemyCurrentHp, int enemyMaxHp) {
        this.playerPokemonId = playerPokemonId;
        this.enemyPokemonId = enemyPokemonId;
        this.playerCurrentHp = playerCurrentHp;
        this.playerMaxHp = playerMaxHp;
        this.enemyCurrentHp = enemyCurrentHp;
        this.enemyMaxHp = enemyMaxHp;
    }

    // ========== GETTER & SETTER ==========
    public int getPlayerPokemonId() { return playerPokemonId; }
    public void setPlayerPokemonId(int playerPokemonId) { this.playerPokemonId = playerPokemonId; }

    public int getEnemyPokemonId() { return enemyPokemonId; }
    public void setEnemyPokemonId(int enemyPokemonId) { this.enemyPokemonId = enemyPokemonId; }

    public int getPlayerCurrentHp() { return playerCurrentHp; }
    public void setPlayerCurrentHp(int playerCurrentHp) { this.playerCurrentHp = playerCurrentHp; }

    public int getPlayerMaxHp() { return playerMaxHp; }
    public void setPlayerMaxHp(int playerMaxHp) { this.playerMaxHp = playerMaxHp; }

    public int getEnemyCurrentHp() { return enemyCurrentHp; }
    public void setEnemyCurrentHp(int enemyCurrentHp) { this.enemyCurrentHp = enemyCurrentHp; }

    public int getEnemyMaxHp() { return enemyMaxHp; }
    public void setEnemyMaxHp(int enemyMaxHp) { this.enemyMaxHp = enemyMaxHp; }

    public Status getPlayerStatus() { return playerStatus; }
    public void setPlayerStatus(Status playerStatus) { this.playerStatus = playerStatus; }

    public Status getEnemyStatus() { return enemyStatus; }
    public void setEnemyStatus(Status enemyStatus) { this.enemyStatus = enemyStatus; }

    // ========== CONVENIENCE METHODS ==========
    public boolean isPlayerAlive() { return playerCurrentHp > 0; }
    public boolean isEnemyAlive() { return enemyCurrentHp > 0; }

    public void reducePlayerHp(int damage) {
        this.playerCurrentHp = Math.max(0, this.playerCurrentHp - damage);
    }

    public void reduceEnemyHp(int damage) {
        this.enemyCurrentHp = Math.max(0, this.enemyCurrentHp - damage);
    }
}