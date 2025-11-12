package com.project03.controller;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Controller
public class OAuthFinalController {

  private static String readCookie(HttpServletRequest req, String name) {
    try {
      Cookie[] cookies = req.getCookies();
      if (cookies == null) return null;
      for (Cookie c : cookies) {
        if (name.equals(c.getName())) {
          return URLDecoder.decode(c.getValue(), StandardCharsets.UTF_8);
        }
      }
    } catch (Exception ignored) {}
    return null;
  }

  @GetMapping(value = "/oauth2/final", produces = MediaType.TEXT_HTML_VALUE)
  public ResponseEntity<String> finalPage(HttpServletRequest request) {
    // DO NOT touch Spring Security here; just render a page and (optionally) deep-link.
    String payloadJson = "{\"authenticated\":true,\"name\":null,\"login\":null,\"email\":null,\"avatar_url\":null}";
    String encoded;
    try {
      encoded = URLEncoder.encode(payloadJson, StandardCharsets.UTF_8);
    } catch (Exception e) {
      encoded = "%7B%22authenticated%22%3Atrue%7D";
    }

    // Prefer ?return_to=..., else cookie from the /oauth2/authorization/* hop
    String returnTo = null;
    try {
      String q = request.getParameter("return_to");
      if (q != null && !q.isBlank()) returnTo = q;
      if (returnTo == null) returnTo = readCookie(request, "oauth_return_to");
    } catch (Exception ignored) {}

    String deepLinkScript = "";
    if (returnTo != null && !returnTo.isBlank()) {
      String safe = returnTo.replace("\"", "");
      deepLinkScript =
          "(function(){try{window.location.replace(\"" + safe + "#userinfo=" + encoded + "\");}catch(e){}})();";
    }

    String html = """
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
              <div>
                <div class="name">Signed in</div>
                <div class="sub">Returning to the app…</div>
              </div>
            </div>
            <div class="muted">If nothing happens, switch back to the app.</div>
          </div>
          <script>
            %s
            (function(){
              try{
                var me = %s;
                if (!location.hash || location.hash.indexOf('#userinfo=') !== 0) {
                  location.replace(location.pathname + '#userinfo=' + encodeURIComponent(JSON.stringify(me)));
                }
                setTimeout(function(){ try{ window.close(); }catch(_){} }, 200);
              }catch(e){}
            })();
          </script>
        </body>
      </html>
      """.replaceFirst("%s", deepLinkScript)
         .replaceFirst("%s", payloadJson);

    return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(html);
  }
}
