package com.example.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.*;

import java.util.List;

@Configuration
public class SecurityConfig {

  @Bean
  SecurityFilterChain security(HttpSecurity http) throws Exception {
    http
      .csrf(csrf -> csrf.disable()) // okay for local JSON POST; add proper CSRF later
      .authorizeHttpRequests(auth -> auth
        .requestMatchers(HttpMethod.POST, "/api/mobile/github/callback").permitAll()
        .requestMatchers("/api/me").authenticated()
        .anyRequest().permitAll()
      )
      .oauth2Login(Customizer.withDefaults())
      .logout(l -> l.logoutUrl("/logout").logoutSuccessUrl("/"))
      .cors(c -> c.configurationSource(corsConfigurationSource()));
    return http.build();
  }

  @Bean
  CorsConfigurationSource corsConfigurationSource() {
    var cfg = new CorsConfiguration();
    cfg.setAllowedOrigins(List.of(
      "http://localhost:19000", // Expo dev UI
      "http://localhost:19006", // Expo web
      "http://10.0.2.2:19000"   // Android emulator → Expo dev
    ));
    cfg.setAllowedMethods(List.of("GET","POST","PUT","DELETE","OPTIONS"));
    cfg.setAllowedHeaders(List.of("*"));
    cfg.setAllowCredentials(true);
    var src = new UrlBasedCorsConfigurationSource();
    src.registerCorsConfiguration("/**", cfg);
    return src;
  }
}
