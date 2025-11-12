package com.project03.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OAuthFinalController {

  @GetMapping("/oauth2/final")
  public String closeTab() {
    // This page:
    // 1) fetches /api/me with cookies
    // 2) shows a minimal "Signed in" UI (avatar + name)
    // 3) stores the user JSON in the URL hash so Expo's openAuthSessionAsync can read it
    // 4) tries to close itself if it's in a WebView
    return """
      <html>
        <head>
          <meta name="viewport" content="width=device-width,initial-scale=1"/>
          <title>Signed in</title>
          <style>
            body { font-family: system-ui, -apple-system, Segoe UI, Roboto, sans-serif; margin: 24px; }
            .card { max-width: 460px; margin: 0 auto; padding: 20px; border: 1px solid #e5e7eb; border-radius: 12px; }
            .row { display:flex; gap:16px; align-items:center; }
            .avatar { width:64px; height:64px; border-radius:50%; background:#eee; object-fit:cover; }
            .name { font-size: 18px; font-weight: 700; margin: 2px 0; }
            .sub { color:#374151; }
            .ok { margin-top:16px; }
            .muted { color:#6b7280; font-size: 12px; margin-top: 8px; }
          </style>
        </head>
        <body>
          <div class="card">
            <div id="loading">Signing you in…</div>
            <div id="signed" style="display:none">
              <div class="row">
                <img id="avatar" class="avatar" src="" alt="avatar"/>
                <div>
                  <div class="name" id="name"></div>
                  <div class="sub" id="login"></div>
                </div>
              </div>
              <div class="ok">
                <button onclick="tryClose()">Close</button>
              </div>
              <div class="muted">You can safely close this tab.</div>
            </div>
          </div>

          <script>
            async function run() {
              try {
                const res = await fetch('/api/me', { credentials: 'include' });
                const me = await res.json();

                if (!me || !me.authenticated) {
                  document.getElementById('loading').innerText = 'Signed in, but no session found.';
                  return;
                }

                // Populate UI
                document.getElementById('loading').style.display = 'none';
                document.getElementById('signed').style.display = 'block';
                document.getElementById('name').innerText = me.name || me.login || 'Signed In';
                document.getElementById('login').innerText = me.login || me.email || '';
                if (me.avatar_url) document.getElementById('avatar').src = me.avatar_url;

                // Put a compact payload in the URL hash so Expo can read it
                const payload = { name: me.name, login: me.login, email: me.email, avatar_url: me.avatar_url, authenticated: true };
                const enc = encodeURIComponent(JSON.stringify(payload));
                if (!location.hash || !location.hash.startsWith('#userinfo=')) {
                  history.replaceState(null, '', location.pathname + '#userinfo=' + enc);
                }

                // If we're in a WebView, try to close; otherwise the user can tap Close
                setTimeout(tryClose, 150);
              } catch (e) {
                document.getElementById('loading').innerText = 'Something went wrong.';
              }
            }

            function tryClose() {
              try { window.close(); } catch(_) {}
            }

            run();
          </script>
        </body>
      </html>
      """;
  }
}
