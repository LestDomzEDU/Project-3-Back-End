package com.project03.security;

import jakarta.servlet.*;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
public class ReturnToCookieFilter implements Filter {
  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {

    HttpServletRequest req = (HttpServletRequest) request;
    HttpServletResponse res = (HttpServletResponse) response;

    if (req.getRequestURI() != null &&
        req.getRequestURI().startsWith("/oauth2/authorization")) {
      String returnTo = req.getParameter("return_to");
      if (returnTo != null && !returnTo.isBlank()) {
        Cookie c = new Cookie("oauth_return_to",
            URLEncoder.encode(returnTo, StandardCharsets.UTF_8));
        c.setHttpOnly(true);
        c.setSecure(true);
        c.setPath("/");
        c.setMaxAge(180); // 3 minutes
        res.addCookie(c);
      }
    }
    chain.doFilter(request, response);
  }
}
