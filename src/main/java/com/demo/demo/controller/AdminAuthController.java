package com.demo.demo.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Tiny dev-mode admin login. Replace with Spring Security for production.
 */
@Controller
public class AdminAuthController {

    // show login form
    @GetMapping("/admin/login")
    public String adminLoginForm() {
        return "admin/login";
    }

    // handle login post
    @PostMapping("/admin/login")
    public String doAdminLogin(@RequestParam String username,
                               @RequestParam String password,
                               HttpSession session,
                               Model model) {
        // dev credentials: admin / secret
        if ("admin".equals(username) && "admin".equals(password)) {
            session.setAttribute("isAdmin", true);
            session.setAttribute("adminName", "Administrator");
            return "redirect:/admin/complaints";
        } else {
            model.addAttribute("error", "Invalid admin credentials");
            return "admin/login";
        }
    }

    // admin logout — clears only admin attributes (keeps user session if you want)
    @GetMapping("/admin/logout")
    public String adminLogout(HttpSession session) {
        session.removeAttribute("isAdmin");
        session.removeAttribute("adminName");
        return "redirect:/";
    }
}