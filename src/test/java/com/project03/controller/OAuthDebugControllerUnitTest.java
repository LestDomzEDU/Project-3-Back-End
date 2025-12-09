package com.project03.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;

class OAuthDebugControllerUnitTest {

    @Test
    @DisplayName("debugClients returns list of client registration metadata")
    void debugClientsReturnsClientList() {
        ClientRegistration github = ClientRegistration
                .withRegistrationId("github")
                .clientId("test-client")
                .clientSecret("secret")
                .scope("read:user")
                .authorizationUri("https://github.com/login/oauth/authorize")
                .tokenUri("https://github.com/login/oauth/access_token")
                .userInfoUri("https://api.github.com/user")
                .userNameAttributeName("id")
                .clientName("GitHub")
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .build();

        InMemoryClientRegistrationRepository repo =
                new InMemoryClientRegistrationRepository(List.of(github));

        OAuthDebugController controller = new OAuthDebugController(repo);

        Map<String, Object> result = controller.debugClients();
        assertThat(result).containsKey("clients");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> clients = (List<Map<String, Object>>) result.get("clients");
        assertThat(clients).hasSize(1);
        assertThat(clients.get(0).get("registrationId")).isEqualTo("github");
        assertThat(clients.get(0).get("clientName")).isEqualTo("GitHub");
    }
}
