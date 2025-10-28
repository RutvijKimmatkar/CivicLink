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

    public User register(String username, String email, String number, String password) {
        if (repo.existsByUsername(username))
            throw new IllegalArgumentException("Username already exists");
        if (repo.existsByEmail(email))
            throw new IllegalArgumentException("Email already exists");

        User u = new User(username, email, number, password);
        return repo.save(u);
    }

    public Optional<User> authenticate(String username, String password) {
        return repo.findByUsername(username)
                .filter(u -> u.getPassword().equals(password));
    }

    public Optional<User> findById(Long id) { return repo.findById(id); }
}