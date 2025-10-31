package com.demo.demo.controller;

import com.demo.demo.model.User;
import com.demo.demo.model.ComplaintStatus;
import com.demo.demo.service.UserService;
import com.demo.demo.service.ComplaintService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * AuthenticationController
 *
 * - login / register endpoints (classic username/password)
 * - a simple /auth/google endpoint that verifies Google idTokens via
 *   Google's tokeninfo endpoint and creates/returns a session.
 *
 * Note: This avoids Spring Security (per request). For production, prefer
 * proper token verification with Google's Java libraries.
 */
@Controller
public class AuthenticationController {

    private final UserService userService;
    private final ComplaintService complaintService;

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

    /**
     * GET /register - shows public register page and some site stats.
     */
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
            // optionally redirect to login with a success flag:
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

    /**
     * POST /auth/google
     * Body: { "idToken": "..." }
     *
     * Verifies token via Google's tokeninfo endpoint:
     *   https://oauth2.googleapis.com/tokeninfo?id_token=XYZ
     *
     * If valid and email_verified is true, find or create a User,
     * persist and set session attributes. Returns JSON { success, message }.
     */
    @PostMapping(path = "/auth/google", produces = "application/json")
    @ResponseBody
    public Map<String, Object> googleAuth(@RequestBody Map<String, String> body, HttpSession session) {
        Map<String, Object> resp = new HashMap<>();

        String idToken = body.get("idToken");
        if (idToken == null || idToken.isBlank()) {
            resp.put("success", false);
            resp.put("message", "Missing idToken");
            return resp;
        }

        // Simple verification using Google's tokeninfo endpoint (no dependency on Google libs)
        String tokenInfoUrl = "https://oauth2.googleapis.com/tokeninfo?id_token=" + idToken;
        RestTemplate rest = new RestTemplate();

        Map<?, ?> tokenInfo;
        try {
            ResponseEntity<Map> r = rest.getForEntity(tokenInfoUrl, Map.class);
            tokenInfo = r.getBody();
            if (tokenInfo == null) {
                resp.put("success", false);
                resp.put("message", "Failed to validate token");
                return resp;
            }
        } catch (Exception ex) {
            resp.put("success", false);
            resp.put("message", "Token verification failed: " + ex.getMessage());
            return resp;
        }

        // tokenInfo now contains claims like: email, email_verified, name, picture, aud (client id), etc.
        Object emailObj = tokenInfo.get("email");
        Object verifiedObj = tokenInfo.get("email_verified");
        Object nameObj = tokenInfo.get("name");
        // optionally check audience ("aud") equals your client id

        if (emailObj == null) {
            resp.put("success", false);
            resp.put("message", "Google token missing email");
            return resp;
        }

        boolean verified = false;
        if (verifiedObj instanceof String) {
            // tokeninfo returns "true"/"false" as strings often
            verified = "true".equalsIgnoreCase((String) verifiedObj);
        } else if (verifiedObj instanceof Boolean) {
            verified = (Boolean) verifiedObj;
        }

        if (!verified) {
            resp.put("success", false);
            resp.put("message", "Google email not verified");
            return resp;
        }

        final String email = emailObj.toString();
        final String name = nameObj != null ? nameObj.toString() : null;

        // find existing user by email
        Optional<User> maybeUser = userService.findByEmail(email);
        User user;
        if (maybeUser.isPresent()) {
            user = maybeUser.get();
        } else {
            // create a new user record using available data
            user = new User();

            // generate a safe username from name or email
            String generated = generateFromEmailOrName(email, name);
            user.setUsername(generated);

            user.setEmail(email);
            // for demo we leave password null or set a random token (no encryption required here)
            user.setPassword(null);
            // number unknown from Google; set empty string or null
            user.setPhoneNumber("");
            // optionally set display name / other fields if your User entity supports them
            // e.g., user.setDisplayName(name);

            user = userService.save(user);
        }

        // now set session attributes (same as normal login)
        session.setAttribute("username", user.getUsername());
        session.setAttribute("userId", user.getId());

        resp.put("success", true);
        resp.put("message", "Logged in");
        return resp;
    }

    // --- helpers ---

    /**
     * Generates a username based on name or email.
     * Example: "John Doe" -> "john.doe", "someone@example.com" -> "someone"
     * Ensures fallback if values missing.
     */
    private String generateFromEmailOrName(String email, String name) {
        if (name != null && !name.isBlank()) {
            // basic sanitize: lowercase and replace non-alphanum with dots, collapse dots
            String s = name.trim().toLowerCase().replaceAll("[^a-z0-9]+", ".").replaceAll("\\.{2,}", ".");
            s = s.replaceAll("^\\.|\\.$", "");
            if (!s.isBlank()) return s;
        }
        if (email != null && !email.isBlank()) {
            int at = email.indexOf('@');
            if (at > 0) return email.substring(0, at).toLowerCase().replaceAll("[^a-z0-9._-]", "");
            return email.toLowerCase().replaceAll("[^a-z0-9._-]", "");
        }
        // final fallback
        return "user" + System.currentTimeMillis() % 100000;
    }
}