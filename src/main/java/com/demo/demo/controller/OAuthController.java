package com.demo.demo.controller;

import com.demo.demo.model.User;
import com.demo.demo.service.GoogleOAuthService;
import com.demo.demo.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/oauth2")
public class OAuthController {

    private final GoogleOAuthService google;
    private final UserService userService;

    @Value("${google.oauth.client-id}")
    private String clientId;

    @Value("${google.oauth.redirect-uri}")
    private String redirectUri;

    public OAuthController(GoogleOAuthService google, UserService userService) {
        this.google = google;
        this.userService = userService;
    }

    @GetMapping("/authorize/google")
    public String authorize(HttpSession session) {
        String state = generateState();
        session.setAttribute("oauth2_state", state);

        String url = UriComponentsBuilder.fromUriString("https://accounts.google.com/o/oauth2/v2/auth")
                .queryParam("response_type", "code")
                .queryParam("client_id", clientId)
                .queryParam("scope", "openid email profile")
                .queryParam("redirect_uri", redirectUri)
                .queryParam("state", state)
                .queryParam("prompt", "select_account consent")
                .queryParam("access_type", "offline")
                .build().toUriString();

        return "redirect:" + url;
    }

    @GetMapping("/callback/google")
    public String callback(@RequestParam(required = false) String code,
                           @RequestParam(required = false) String state,
                           @RequestParam(required = false) String error,
                           HttpSession session,
                           Model model) {

        if (error != null) {
            model.addAttribute("error", "Google returned error: " + error);
            return "login";
        }

        String expected = (String) session.getAttribute("oauth2_state");
        session.removeAttribute("oauth2_state");
        if (expected == null || !expected.equals(state)) {
            model.addAttribute("error", "Invalid state (possible CSRF).");
            return "login";
        }

        try {
            GoogleOAuthService.TokenResponse tokens = google.exchangeCode(code);
            Map<String, Object> userInfo = google.fetchUserInfo(tokens.accessToken);

            String email = (String) userInfo.get("email");
            Boolean emailVerified = userInfo.get("email_verified") != null ? Boolean.valueOf(userInfo.get("email_verified").toString()) : Boolean.FALSE;
            String name = (String) userInfo.get("name");
            String picture = (String) userInfo.get("picture");
            String googleId = (String) userInfo.get("sub");

            Optional<User> maybe = userService.findByEmail(email);
            User user;
            if (maybe.isPresent()) {
                user = maybe.get();
                user.setGoogleId(googleId);
                user.setPictureUrl(picture);
                user.setEmailVerified(emailVerified);
                userService.save(user);
            } else {
                user = new User();
                user.setEmail(email);
                user.setUsername(email.split("@")[0]);
                user.setUsername(name);
                user.setGoogleId(googleId);
                user.setPictureUrl(picture);
                user.setEmailVerified(emailVerified);
                // if password required in your model, leave null — user logs in via Google
                user = userService.save(user);
            }

            session.setAttribute("username", user.getUsername());
            session.setAttribute("userId", user.getId());

            return "redirect:/dashboard";
        } catch (Exception ex) {
            model.addAttribute("error", "OAuth failed: " + ex.getMessage());
            return "login";
        }
    }

    private String generateState() {
        byte[] b = new byte[24];
        new SecureRandom().nextBytes(b);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
    }
}