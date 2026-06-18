package pokemonbattle.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpSession; 
import pokemonbattle.database.UserDAO;
import pokemonbattle.models.User;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*") 
public class AuthController {

    private UserDAO userDAO = new UserDAO();

    // ===== ENDPOINT REGISTER =====
    @PostMapping("/register")
    public Map<String, Object> register(@RequestBody User user) {
        Map<String, Object> response = new HashMap<>();
        
        if (user.getUsername() == null || user.getUsername().trim().isEmpty() ||
            user.getPassword() == null || user.getPassword().trim().isEmpty()) {
            response.put("success", false);
            response.put("message", "Username dan password tidak boleh kosong!");
            return response;
        }

        boolean isSuccess = userDAO.registerUser(user);
        
        if (isSuccess) {
            response.put("success", true);
            response.put("message", "Akun berhasil didaftarkan! Silakan login.");
        } else {
            response.put("success", false);
            response.put("message", "Gagal mendaftarkan akun. Username mungkin sudah dipakai!");
        }
        
        return response;
    }

    // ===== ENDPOINT LOGIN (WITH SESSION) =====
    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody User user, HttpSession session) {
        Map<String, Object> response = new HashMap<>();

        User loggedInUser = userDAO.loginUser(user.getUsername(), user.getPassword());
        
        if (loggedInUser != null) {
            session.setAttribute("currentUser", loggedInUser.getUsername());
            
            response.put("success", true);
            response.put("message", "Login berhasil! Selamat datang.");
            response.put("username", loggedInUser.getUsername());
        } else {
            response.put("success", false);
            response.put("message", "Username atau password salah!");
        }
        
        return response;
    }

    // ===== ENDPOINT LOGOUT =====
    @PostMapping("/logout")
    public Map<String, Object> logout(HttpSession session) {
        session.invalidate(); // Menghapus session saat keluar
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Berhasil logout.");
        return response;
    }
}