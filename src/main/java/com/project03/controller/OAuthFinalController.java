package com.project03.controller;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Controller
public class OAuthFinalController {

  private static String readCookie(HttpServletRequest req, String name) {
    try {
      if (req.getCookies() == null) return null;
      for (Cookie c : req.getCookies()) {
        if (name.equals(c.getName())) {
          return URLDecoder.decode(c.getValue(), StandardCharsets.UTF_8);
        }
      }
    } catch (Exception ignored) {}
    return null;
  }

  private static String j(String s) {
    if (s == null) return "null";
    return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
  }

  @GetMapping(value = "/oauth2/final", produces = MediaType.TEXT_HTML_VALUE)
  @ResponseBody
  public String finalPage(HttpServletRequest request, Authentication authentication) {
    // Default/minimal payload; we will try to enhance from Authentication safely.
    String name = "";
    String login = "";
    String email = "";
    String avatar = "";

    try {
      if (authentication != null && authentication.getPrincipal() instanceof OAuth2User o) {
        Map<String, Object> a = o.getAttributes();

        // GitHub fields
        if (a.containsKey("login")) login = String.valueOf(a.get("login"));
        if (a.containsKey("name"))  name  = String.valueOf(a.get("name"));
        if (a.containsKey("avatar_url")) avatar = String.valueOf(a.get("avatar_url"));

        // Common / Google OIDC
        if (a.containsKey("email")) email = String.valueOf(a.get("email"));
        if (a.containsKey("picture")) avatar = String.valueOf(a.get("picture"));
        if ((name == null || name.isEmpty())
            && (a.containsKey("given_name") || a.containsKey("family_name"))) {
          String gn = String.valueOf(a.getOrDefault("given_name",""));
          String fn = String.valueOf(a.getOrDefault("family_name",""));
          name = (gn + " " + fn).trim();
        }
        if ((login == null || login.isEmpty()) && email != null) {
          int at = email.indexOf('@');
          if (at > 0) login = email.substring(0, at);
        }
      }
    } catch (Exception ignored) {
      // Do not fail this endpoint; just show a minimal success page.
    }

    String json = "{\"authenticated\":true," +
        "\"name\":" + j(name) + "," +
        "\"login\":" + j(login) + "," +
        "\"email\":" + j(email) + "," +
        "\"avatar_url\":" + j(avatar) + "}";

    String encoded = URLEncoder.encode(json, StandardCharsets.UTF_8);

    // Prefer query param, else cookie set earlier by your filter during /oauth2/authorization/*
    String returnTo = request.getParameter("return_to");
    if (returnTo == null || returnTo.isBlank()) {
      returnTo = readCookie(request, "oauth_return_to");
    }

    String deepLinkScript = "";
    try {
      if (returnTo != null && !returnTo.isBlank()) {
        String safe = returnTo.replace("\"", "");
        deepLinkScript = "(function(){try{window.location.replace(\"" + safe + "#userinfo=" + encoded + "\");}catch(e){}})();";
      }
    } catch (Exception ignored) {}

    // Always return a success page; never throw from here.
    return """
      <!doctype html>
      <html>
        <head>
          <meta name="viewport" content="width=device-width,initial-scale=1"/>
          <title>Signed in</title>
          <style>
            body{font-family:system-ui,-apple-system,Segoe UI,Roboto,sans-serif;margin:24px;}
            .card{max-width:460px;margin:0 auto;padding:20px;border:1px solid #e5e7eb;border-radius:12px}
            .row{display:flex;gap:16px;align-items:center}
            .avatar{width:64px;height:64px;border-radius:50%;background:#eee;object-fit:cover}
            .name{font-size:18px;font-weight:700;margin:2px 0}
            .sub{color:#374151}
            .muted{color:#6b7280;font-size:12px;margin-top:8px}
          </style>
        </head>
        <body>
          <div class="card">
            <div class="row">
              <img id="avatar" class="avatar" alt="avatar"/>
              <div>
                <div class="name" id="name"></div>
                <div class="sub" id="login"></div>
              </div>
            </div>
            <div class="muted">You can return to the app now.</div>
          </div>
          <script>
            %s
            (function(){
              try{
                var me = %s;
                if (me && me.avatar_url) document.getElementById('avatar').src = me.avatar_url;
                document.getElementById('name').innerText = me.name || me.login || 'Signed In';
                document.getElementById('login').innerText = me.login || me.email || '';
                if (!location.hash || location.hash.indexOf('#userinfo=') !== 0) {
                  location.replace(location.pathname + '#userinfo=' + encodeURIComponent(JSON.stringify(me)));
                }
                setTimeout(function(){ try{ window.close(); }catch(_){} }, 200);
              }catch(e){}
            })();
          </script>
        </body>
      </html>
      """.formatted(deepLinkScript, json);
  }
}
