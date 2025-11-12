package com.project03.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class OAuthSecurityConfig {

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http,
                                         ClientRegistrationRepository repo,
                                         OAuthSuccessHandler successHandler) throws Exception {
    http
      .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**", "/h2/**"))
      .headers(h -> h.frameOptions(f -> f.sameOrigin()))
      .cors(Customizer.withDefaults())
      .authorizeHttpRequests(auth -> auth
        .requestMatchers("/", "/oauth2/**", "/api/me", "/api/logout", "/h2/**").permitAll()
        .anyRequest().authenticated()
      )
      .oauth2Login(oauth -> oauth
        .authorizationEndpoint(a -> a.baseUri("/oauth2/authorization"))
        .successHandler(successHandler)      // ← always go to /oauth2/final
        .failureUrl("/")
      );
    return http.build();
  }

  @Bean
  CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOriginPatterns(List.of("*"));
    config.setAllowCredentials(true);
    config.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));
    config.setAllowedHeaders(List.of("Authorization","Cache-Control","Content-Type","X-Requested-With"));
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return source;
  }
}
