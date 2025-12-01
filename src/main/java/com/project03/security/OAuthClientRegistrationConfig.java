package com.project03.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;

@Configuration
public class OAuthClientRegistrationConfig {

  // IMPORTANT:
  // This should point at your deployed backend base URL in prod.
  // It defaults to your Heroku app, but you can override locally with
  // OAUTH_REDIRECT_BASE=http://localhost:8080 for local testing.
  @Value("${OAUTH_REDIRECT_BASE:https://grad-quest-app-2cac63f2b9b2.herokuapp.com}")
  private String redirectBase;

  // GitHub OAuth (unchanged)
  @Value("${GITHUB_CLIENT_ID:}")
  private String githubClientId;

  @Value("${GITHUB_CLIENT_SECRET:}")
  private String githubClientSecret;

  // Discord OAuth (new)
  @Value("${DISCORD_CLIENT_ID:}")
  private String discordClientId;

  @Value("${DISCORD_CLIENT_SECRET:}")
  private String discordClientSecret;

  @Bean
  public ClientRegistrationRepository clientRegistrationRepository() {
    // ---- GitHub registration ----
    ClientRegistration github = ClientRegistration
        .withRegistrationId("github")
        .clientId(githubClientId)
        .clientSecret(githubClientSecret)
        .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
        .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
        // Spring’s default login callback: {baseUrl}/login/oauth2/code/{registrationId}
        .redirectUri(redirectBase + "/login/oauth2/code/{registrationId}")
        .scope("read:user", "user:email")
        .authorizationUri("https://github.com/login/oauth/authorize")
        .tokenUri("https://github.com/login/oauth/access_token")
        .userInfoUri("https://api.github.com/user")
        .userNameAttributeName("id")
        .clientName("GitHub")
        .build();

    // ---- Discord registration (replaces Google) ----
    ClientRegistration discord = ClientRegistration
        .withRegistrationId("discord")
        .clientId(discordClientId)
        .clientSecret(discordClientSecret)
        // Discord expects client_id / client_secret in the POST body
        .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
        .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
        // MUST match the redirect you configure in the Discord Developer Portal
        // Example: https://grad-quest-app-2cac63f2b9b2.herokuapp.com/login/oauth2/code/discord
        .redirectUri(redirectBase + "/login/oauth2/code/{registrationId}")
        .scope("identify", "email")
        .authorizationUri("https://discord.com/api/oauth2/authorize")
        .tokenUri("https://discord.com/api/oauth2/token")
        .userInfoUri("https://discord.com/api/users/@me")
        .userNameAttributeName("id")
        .clientName("Discord")
        .build();

    // We now support two providers: github and discord
    return new InMemoryClientRegistrationRepository(github, discord);
  }
}
