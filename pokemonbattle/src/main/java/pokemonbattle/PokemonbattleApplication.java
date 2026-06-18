package pokemonbattle;

import org.springframework.boot.SpringApplication; // Import disesuaikan dengan package baru kamu
import org.springframework.boot.autoconfigure.SpringBootApplication;

import pokemonbattle.database.InitDB;

@SpringBootApplication
public class PokemonbattleApplication {

	public static void main(String[] args) {
		InitDB.initialize();
		
		SpringApplication.run(PokemonbattleApplication.class, args);
	}
}