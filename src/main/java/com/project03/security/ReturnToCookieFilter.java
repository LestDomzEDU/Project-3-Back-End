package com.project03.security;

import jakarta.servlet.*;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * When the user hits /oauth2/authorization/{provider}?return_to=...
 * we persist that deep link in a short-lived cookie so /oauth2/final can read it.
 */
@Component
public class ReturnToCookieFilter implements Filter {

  private static final String COOKIE_NAME = "oauth_return_to";

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {

    HttpServletRequest req = (HttpServletRequest) request;
    HttpServletResponse res = (HttpServletResponse) response;

    String uri = req.getRequestURI();
    String returnTo = req.getParameter("return_to");

    if (uri != null && uri.startsWith("/oauth2/authorization") && returnTo != null && !returnTo.isBlank()) {
      String enc = URLEncoder.encode(returnTo, StandardCharsets.UTF_8);
      Cookie c = new Cookie(COOKIE_NAME, enc);
      c.setHttpOnly(true);
      c.setSecure(true);
      c.setPath("/");        // available to /oauth2/final
      c.setMaxAge(180);      // 3 minutes
      res.addCookie(c);
    }

    chain.doFilter(request, response);
  }
}
