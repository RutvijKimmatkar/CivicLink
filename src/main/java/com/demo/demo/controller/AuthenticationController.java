package com.demo.demo.controller;

import com.demo.demo.model.User;
import com.demo.demo.model.ComplaintStatus;
import com.demo.demo.service.UserService;
import com.demo.demo.service.ComplaintService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class AuthenticationController {

    private final UserService userService;
    private final ComplaintService complaintService; // <- injected

    // constructor injection: Spring will wire both services
    public AuthenticationController(UserService userService, ComplaintService complaintService) {
        this.userService = userService;
        this.complaintService = complaintService;
    }

    @GetMapping({"/", "/login"})
    public String loginForm(Model model) {
        return "login";
    }

    @PostMapping("/login")
    public String doLogin(@RequestParam String username,
                          @RequestParam String password,
                          HttpSession session,
                          Model model) {
        var maybe = userService.authenticate(username, password);
        if (maybe.isPresent()) {
            User u = maybe.get();
            session.setAttribute("username", u.getUsername());
            session.setAttribute("userId", u.getId());
            return "redirect:/dashboard";
        } else {
            model.addAttribute("error", "Invalid username or password");
            return "login";
        }
    }

    @GetMapping("/register")
    public String registerForm(Model model) {
        long total = complaintService.countAll();
        long solved = complaintService.countByStatus(ComplaintStatus.COMPLETED);
        long pending = total - solved;
        double percentage = total > 0 ? (solved * 100.0 / total) : 0.0;

        model.addAttribute("totalComplaints", total);
        model.addAttribute("solvedComplaints", solved);
        model.addAttribute("pendingComplaints", pending);
        model.addAttribute("resolutionPercent", percentage);

        return "register";
    }

    @PostMapping("/register")
    public String doRegister(@RequestParam String username,
                             @RequestParam String email,
                             @RequestParam String number,
                             @RequestParam String password,
                             Model model) {
        try {
            userService.register(username, email, number, password);
            model.addAttribute("message", "Registration successful. Please login.");
            return "login";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            return "register";
        }
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }
}