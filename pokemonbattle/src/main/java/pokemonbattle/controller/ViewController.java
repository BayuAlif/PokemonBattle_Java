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
     @GetMapping("/collection")
    public String collectionPage() {
        return "collection"; 
    }

    @GetMapping("/register")
    public String registerPage() {
        return "register";
    }

    @GetMapping("/home")
    public String homePage() {
        return "home"; 
    }

    @GetMapping("/battle")
    public String battleInGame() {
        return "battle"; 
    }

    @GetMapping("/inventory")
    public String inventoryPage() {
        return "inventory"; 
    }
}