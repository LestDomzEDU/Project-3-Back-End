package com.example.auth;

import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;

@RestController
@RequestMapping("/api/mobile/github")
public class MobileGithubAuthController {

  @Value("${spring.security.oauth2.client.registration.github.client-id}")
  String clientId;

  @Value("${spring.security.oauth2.client.registration.github.client-secret}")
  String clientSecret;

  record CodeDTO(String code, String redirectUri) {}

  private final WebClient ghAuth = WebClient.builder()
      .baseUrl("https://github.com")
      .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
      .build();

  private final WebClient ghApi = WebClient.builder()
      .baseUrl("https://api.github.com")
      .defaultHeader("X-GitHub-Api-Version", "2022-11-28")
      .build();

  @PostMapping("/callback")
  public ResponseEntity<?> callback(@RequestBody CodeDTO dto, HttpServletRequest req) {
    if (dto == null || dto.code() == null || dto.redirectUri() == null) {
      return ResponseEntity.badRequest().body("Missing code/redirectUri");
    }

    // 1) Exchange authorization code → access_token
    Map<String, Object> tokenRes = ghAuth.post()
        .uri("/login/oauth/access_token")
        .bodyValue(Map.of(
          "client_id", clientId,
          "client_secret", clientSecret,
          "code", dto.code(),
          "redirect_uri", dto.redirectUri()))
        .retrieve()
        .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
        .block();

    String accessToken = tokenRes == null ? null : (String) tokenRes.get("access_token");
    if (accessToken == null) {
      return ResponseEntity.status(400).body("No access_token from GitHub");
    }

    // 2) Fetch GitHub user
    Map<String, Object> ghUser = ghApi.get()
        .uri("/user")
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
        .retrieve()
        .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
        .block();

    if (ghUser == null || ghUser.get("login") == null) {
      return ResponseEntity.status(400).body("Failed to load GitHub user");
    }

    // 3) Create Authentication & HTTP session
    var principal = (String) ghUser.get("login");
    var auth = new UsernamePasswordAuthenticationToken(
        principal, "N/A", List.of(new SimpleGrantedAuthority("ROLE_USER")));
    SecurityContextHolder.getContext().setAuthentication(auth);
    req.getSession(true); // ensures Set-Cookie on response

    return ResponseEntity.ok(Map.of("ok", true, "login", principal));
  }
}
