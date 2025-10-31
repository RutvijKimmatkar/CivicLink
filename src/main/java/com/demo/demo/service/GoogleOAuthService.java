package com.demo.demo.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Service
public class GoogleOAuthService {

    @Value("${google.oauth.client-id}")
    private String clientId;

    @Value("${google.oauth.client-secret}")
    private String clientSecret;

    @Value("${google.oauth.redirect-uri}")
    private String redirectUri;

    private final RestTemplate rest = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();

    public static class TokenResponse {
        @JsonProperty("access_token") public String accessToken;
        @JsonProperty("expires_in") public Long expiresIn;
        @JsonProperty("refresh_token") public String refreshToken;
        @JsonProperty("scope") public String scope;
        @JsonProperty("id_token") public String idToken;
        @JsonProperty("token_type") public String tokenType;
    }

    public TokenResponse exchangeCode(String code) throws Exception {
        String url = "https://oauth2.googleapis.com/token";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        String body = "code=" + encode(code) +
                "&client_id=" + encode(clientId) +
                "&client_secret=" + encode(clientSecret) +
                "&redirect_uri=" + encode(redirectUri) +
                "&grant_type=authorization_code";

        HttpEntity<String> request = new HttpEntity<>(body, headers);
        ResponseEntity<String> resp = rest.postForEntity(url, request, String.class);

        if (!resp.getStatusCode().is2xxSuccessful()) {
            throw new IllegalStateException("Token endpoint returned " + resp.getStatusCode() + ": " + resp.getBody());
        }
        return mapper.readValue(resp.getBody(), TokenResponse.class);
    }

    /**
     * Fetch userinfo using the access token (OpenID Connect userinfo endpoint).
     */
    public Map<String, Object> fetchUserInfo(String accessToken) throws Exception {
        String url = "https://openidconnect.googleapis.com/v1/userinfo";
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<Void> req = new HttpEntity<>(headers);
        ResponseEntity<String> resp = rest.exchange(url, HttpMethod.GET, req, String.class);
        if (!resp.getStatusCode().is2xxSuccessful()) {
            throw new IllegalStateException("Userinfo endpoint returned " + resp.getStatusCode() + ": " + resp.getBody());
        }
        return mapper.readValue(resp.getBody(), Map.class);
    }

    private String encode(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}