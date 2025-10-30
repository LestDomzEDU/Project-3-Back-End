// src/main/java/com/project03/auth/LogoutController.java
package com.project03.auth;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
public class LogoutController {

  @PostMapping("/api/logout")
  public ResponseEntity<?> logout(HttpServletRequest req) {
    var session = req.getSession(false);
    if (session != null) session.invalidate();
    SecurityContextHolder.clearContext();
    return ResponseEntity.ok(Map.of("ok", true));
  }
}
