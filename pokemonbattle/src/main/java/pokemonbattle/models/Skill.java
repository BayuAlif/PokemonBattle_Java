package pokemonbattle.models;

public class Skill {
    private int id;
    private String name;
    private PokemonType type;
    private int power;
    private int accuracy;

    public Skill() {}

    public Skill(int id, String name, PokemonType type, int power, int accuracy) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.power = power;
        this.accuracy = accuracy;
    }

    // getter & setter
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public PokemonType getType() { return type; }
    public void setType(PokemonType type) { this.type = type; }
    public int getPower() { return power; }
    public void setPower(int power) { this.power = power; }
    public int getAccuracy() { return accuracy; }
    public void setAccuracy(int accuracy) { this.accuracy = accuracy; }
}