package com.demo.demo.service;

import com.demo.demo.model.User;
import com.demo.demo.repo.UserRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {

    private final UserRepository repo;

    public UserService(UserRepository repo) {
        this.repo = repo;
    }

    /* ===============================
       NORMAL REGISTRATION & LOGIN
       =============================== */

    // --- Register new user (manual signup) ---
    public User register(String username, String email, String number, String password) {
        if (repo.existsByUsername(username))
            throw new IllegalArgumentException("Username already exists");
        if (repo.existsByEmail(email))
            throw new IllegalArgumentException("Email already exists");

        User u = new User();
        u.setUsername(username);
        u.setEmail(email);
        u.setPhoneNumber(number);
        u.setPassword(password);
        u.setEmailVerified(false);

        return repo.save(u);
    }

    // --- Standard username/password authentication ---
    public Optional<User> authenticate(String username, String password) {
        return repo.findByUsername(username)
                .filter(u -> u.getPassword() != null && u.getPassword().equals(password));
    }

    // --- Fetch by ID (used across dashboards) ---
    public Optional<User> findById(Long id) {
        return repo.findById(id);
    }

    /* ===============================
       GOOGLE LOGIN INTEGRATION
       =============================== */

    // --- Find by email (used by Google OAuth) ---
    public Optional<User> findByEmail(String email) {
        return repo.findByEmail(email);
    }

    // --- Save or update user (for OAuth-created accounts) ---
    public User save(User user) {
        return repo.save(user);
    }

    // --- Create or update Google user ---
    public User upsertGoogleUser(String googleId, String email, String name, String pictureUrl) {
        Optional<User> existing = repo.findByEmail(email);

        if (existing.isPresent()) {
            // Update existing Google-linked user
            User u = existing.get();
            u.setGoogleId(googleId);
            u.setPictureUrl(pictureUrl);
            u.setEmailVerified(true);
            return repo.save(u);
        } else {
            // Create new Google user record
            User u = new User();
            u.setUsername(name);
            u.setEmail(email);
            u.setGoogleId(googleId);
            u.setPictureUrl(pictureUrl);
            u.setPassword(null); // Google users don't have local password
            u.setEmailVerified(true);
            return repo.save(u);
        }
    }
}