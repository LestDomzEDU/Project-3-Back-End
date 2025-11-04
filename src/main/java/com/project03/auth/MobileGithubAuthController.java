package com.project03.auth;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
public class MobileGithubAuthController {

  private final RestTemplate http;
  private final String clientId;
  private final String clientSecret;

  // Simple in-memory session store for local/dev
  private static final ConcurrentHashMap<String, Map<String, Object>> SESSION_STORE = new ConcurrentHashMap<>();

  public MobileGithubAuthController(RestTemplate http,
                                    @Value("${github.client-id}") String clientId,
                                    @Value("${github.client-secret}") String clientSecret) {
    this.http = http;
    this.clientId = clientId;
    this.clientSecret = clientSecret;
  }

  // ---------- GITHUB CALLBACK ----------
  @GetMapping("/oauth/github/callback")
  public void githubCallback(
      @RequestParam("code") String code,
      @RequestParam("state") String state,
      HttpServletResponse resp
  ) throws IOException {
    // 1) Exchange code → token
    String token = exchangeCodeForToken(code, "http://10.0.2.2:8080/oauth/github/callback");

    // 2) Fetch GitHub user
    Map<String, Object> user = fetchGithubUser(token);

    // 3) Store session
    SESSION_STORE.put(state, Map.of(
        "authenticated", true,
        "login", user.getOrDefault("login", "unknown"),
        "name",  user.getOrDefault("name",  user.getOrDefault("login", "unknown"))
    ));

    // 4) Set a cookie so visiting "/" shows the success UI
    Cookie cookie = new Cookie("sessionId", state);
    cookie.setPath("/");
    cookie.setHttpOnly(false); // local/dev only
    cookie.setSecure(false);   // local HTTP
    resp.addCookie(cookie);

    // 5) Show success page and try to deep-link
    String deepLink = "gradquest://oauth?session=" + state;
    resp.setStatus(200);
    resp.setContentType("text/html;charset=UTF-8");
    resp.getWriter().write(
        "<!doctype html><html><head><meta charset='utf-8'/>" +
        "<title>Logged in</title>" +
        "<meta name='viewport' content='width=device-width,initial-scale=1'/>" +
        "<meta http-equiv='refresh' content='0;url=" + deepLink + "'>" +
        "<style>body{font:17px/1.5 -apple-system,Roboto,system-ui,sans-serif;margin:32px;color:#222}" +
        ".box{max-width:720px;margin:0 auto;padding:28px;border:1px solid #e5e7eb;border-radius:14px;box-shadow:0 2px 10px rgba(0,0,0,.05)}" +
        "h1{font-size:28px;margin:0 0 10px}" +
        "button,a.btn{display:inline-block;background:#2563eb;color:#fff;padding:12px 18px;border-radius:10px;border:0;text-decoration:none;margin-right:10px}" +
        ".ghost{background:#fff;color:#111;border:1px solid #cbd5e1}" +
        "</style></head><body>" +
        "<div class='box'>" +
        "<h1>Logged in successfully</h1>" +
        "<p>You can return to the app or log out here.</p>" +
        "<a class='btn' href='" + deepLink + "'>Return to App</a>" +
        "<form method='post' action='/api/logout' style='display:inline'>" +
        "<button class='btn ghost' type='submit'>Log out</button></form>" +
        "</div>" +
        "<script>location.href='" + deepLink + "';</script>" +
        "</body></html>"
    );
  }

  // ---------- SESSION CHECK FOR THE APP ----------
  @GetMapping("/api/mobile/github/session")
  public ResponseEntity<?> session(@RequestParam("session") String session) {
    Map<String, Object> data = SESSION_STORE.get(session);
    if (data == null) return ResponseEntity.ok(Map.of("authenticated", false));
    return ResponseEntity.ok(data);
  }

  // ---------- LOG OUT ----------
  @PostMapping("/api/logout")
  public ResponseEntity<String> logout(@CookieValue(value = "sessionId", required = false) String sessionId,
                                       HttpServletResponse resp) {
    if (sessionId != null) {
      SESSION_STORE.remove(sessionId);
    }
    Cookie c = new Cookie("sessionId", "");
    c.setPath("/");
    c.setMaxAge(0);
    resp.addCookie(c);

    String html =
        "<!doctype html><html><head><meta charset='utf-8'/>" +
        "<meta name='viewport' content='width=device-width,initial-scale=1'/>" +
        "<title>Logged out</title>" +
        "<style>body{font:17px/1.5 -apple-system,Roboto,system-ui,sans-serif;margin:32px;color:#222}" +
        ".box{max-width:720px;margin:0 auto;padding:28px;border:1px solid #e5e7eb;border-radius:14px;box-shadow:0 2px 10px rgba(0,0,0,.05)}" +
        "a.btn{display:inline-block;background:#2563eb;color:#fff;padding:12px 18px;border-radius:10px;text-decoration:none}" +
        "</style></head><body><div class='box'>" +
        "<h1>You are logged out</h1>" +
        "<a class='btn' href='/'>Go home</a>" +
        "</div></body></html>";

    return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(html);
  }

  // ---------- HELPERS ----------
  private String exchangeCodeForToken(String code, String redirectUri) {
    String url = "https://github.com/login/oauth/access_token";

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
    headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));

    MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
    form.add("client_id", clientId);
    form.add("client_secret", clientSecret);
    form.add("code", code);
    form.add("redirect_uri", redirectUri);

    ResponseEntity<Map> resp = http.postForEntity(url, new HttpEntity<>(form, headers), Map.class);
    if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null || resp.getBody().get("access_token") == null) {
      throw new RuntimeException("Token exchange failed: " + resp.getStatusCode());
    }
    return resp.getBody().get("access_token").toString();
  }

  private Map<String, Object> fetchGithubUser(String accessToken) {
    String url = "https://api.github.com/user";
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(accessToken);
    headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));

    ResponseEntity<Map> resp = http.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), Map.class);
    if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
      throw new RuntimeException("User fetch failed: " + resp.getStatusCode());
    }
    return resp.getBody();
  }
}
