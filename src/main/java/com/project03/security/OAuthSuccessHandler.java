package com.project03.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

@Component
public class OAuthSuccessHandler implements AuthenticationSuccessHandler {

  private static String cookie(HttpServletRequest req, String name) {
    if (req.getCookies() == null) return null;
    for (Cookie c : req.getCookies()) {
      if (name.equals(c.getName())) {
        return URLDecoder.decode(c.getValue(), StandardCharsets.UTF_8);
      }
    }
    return null;
  }

  @Override
  public void onAuthenticationSuccess(
      HttpServletRequest request,
      HttpServletResponse response,
      Authentication authentication) {

    String returnTo = request.getParameter("return_to");
    if (returnTo == null || returnTo.isBlank()) {
      returnTo = cookie(request, "oauth_return_to");
    }

    String target = (returnTo == null || returnTo.isBlank())
        ? "/oauth2/final"
        : "/oauth2/final?return_to=" + java.net.URLEncoder.encode(returnTo, StandardCharsets.UTF_8);

    try {
      response.sendRedirect(target);
    } catch (Exception e) {
      // Last-resort fallback
      try { response.sendRedirect("/oauth2/final"); } catch (Exception ignored) {}
    }
  }
}
