package com.project03.api;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
public class MeController {
  @GetMapping("/api/me")
  public Map<String,Object> me(Authentication auth) {
    return (auth == null)
      ? Map.of("authenticated", false)
      : Map.of("authenticated", true, "name", auth.getName());
  }
}
