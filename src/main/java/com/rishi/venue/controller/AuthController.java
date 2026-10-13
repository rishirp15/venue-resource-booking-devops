
package com.rishi.venue.controller;

import com.rishi.venue.model.User;
import com.rishi.venue.repository.UserRepository;
import com.rishi.venue.service.PasswordService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Locale;
import java.util.Optional;

@Controller
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordService passwordService;

    public AuthController(
            UserRepository userRepository,
            PasswordService passwordService) {

        this.userRepository = userRepository;
        this.passwordService = passwordService;
    }

    // ================= REGISTRATION =================

    @GetMapping("/register")
    public String showRegisterPage() {
        return "register";
    }

    @PostMapping("/register")
    public String register(
            @RequestParam String name,
            @RequestParam String email,
            @RequestParam String password,
            Model model) {

        name = name.trim();
        email = email.trim().toLowerCase(Locale.ROOT);

        if (name.isBlank() || email.isBlank() || password.isBlank()) {
            model.addAttribute(
                    "error",
                    "All fields are required."
            );
            return "register";
        }

        if (userRepository.existsByEmail(email)) {
            model.addAttribute(
                    "error",
                    "An account with this email already exists."
            );
            return "register";
        }

        User user = new User();
        user.setName(name);
        user.setEmail(email);

        // Store BCrypt hash, not plaintext password.
        user.setPassword(
                passwordService.hashPassword(password)
        );

        user.setRole("USER");

        userRepository.save(user);

        return "redirect:/login?registered";
    }

    // ================= LOGIN =================

    @GetMapping("/login")
    public String showLoginPage() {
        return "login";
    }

    @PostMapping("/login")
    public String login(
            @RequestParam String email,
            @RequestParam String password,
            HttpServletRequest request,
            Model model) {

        email = email.trim().toLowerCase(Locale.ROOT);

        Optional<User> optionalUser =
                userRepository.findByEmail(email);

        if (optionalUser.isEmpty()) {
            model.addAttribute(
                    "error",
                    "Invalid email or password."
            );
            return "login";
        }

        User user = optionalUser.get();

        if (!passwordService.matches(
                password,
                user.getPassword())) {

            model.addAttribute(
                    "error",
                    "Invalid email or password."
            );
            return "login";
        }

        // Create a session if one does not exist.
        HttpSession session = request.getSession();

        // Rotate the session ID after authentication.
        request.changeSessionId();

        // Store authenticated user information.
        session.setAttribute("userId", user.getId());
        session.setAttribute("userName", user.getName());
        session.setAttribute("userRole", user.getRole());

        return "redirect:/";
    }

    // ================= LOGOUT =================

    @PostMapping("/logout")
    public String logout(HttpServletRequest request) {

        HttpSession session = request.getSession(false);

        if (session != null) {
            session.invalidate();
        }

        return "redirect:/login?logout";
    }
}
