package pokemonbattle.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ViewController {

    @GetMapping("/")
    public String index() {
        return "login"; 
    }

    @GetMapping("/login")
    public String loginPage() {
        return "login"; 
    }

    @GetMapping("/register")
    public String registerPage() {
        return "register";
    }

    @GetMapping("/home")
    public String homePage() {
        return "home"; 
    }

    @GetMapping("/battle-ingame")
    public String battleInGame() {
        return "battle-ingame"; 
    }

    @GetMapping("/inventory")
    public String inventoryPage() {
        return "inventory"; 
    }
}