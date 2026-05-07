package net.disc0.sonshine_inventory.controller;

import net.disc0.sonshine_inventory.dao.UserRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/setup")
public class SetupController {

    private final UserRepository userRepository;
    private final UserDetailsManager userDetailsManager;
    private final PasswordEncoder passwordEncoder;

    public SetupController(UserRepository userRepository,
                           UserDetailsManager userDetailsManager,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.userDetailsManager = userDetailsManager;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping
    public String setupPage(Model model) {
        if (userRepository.count() > 0) {
            return "redirect:/admin";
        }
        return "setup";
    }

    @PostMapping
    public String createFirstAdmin(@RequestParam String username,
                                   @RequestParam String displayName,
                                   @RequestParam String password,
                                   @RequestParam String confirmPassword,
                                   Model model) {
        if (userRepository.count() > 0) {
            return "redirect:/admin";
        }

        if (username.isBlank() || displayName.isBlank() || password.isBlank()) {
            model.addAttribute("error", "All fields are required.");
            return "setup";
        }

        if (!password.equals(confirmPassword)) {
            model.addAttribute("error", "Passwords do not match.");
            model.addAttribute("username", username);
            model.addAttribute("displayName", displayName);
            return "setup";
        }

        UserDetails newUser = User.withUsername(username)
                .password(passwordEncoder.encode(password))
                .roles("ADMIN")
                .build();
        userDetailsManager.createUser(newUser);

        userRepository.findById(username).ifPresent(u -> {
            u.setDisplayName(displayName);
            userRepository.save(u);
        });

        return "redirect:/admin";
    }
}