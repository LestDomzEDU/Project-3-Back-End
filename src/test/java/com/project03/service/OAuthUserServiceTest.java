package com.project03.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.project03.model.User;
import com.project03.repository.UserRepository;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.user.OAuth2User;

class OAuthUserServiceTest {

    private OAuth2User mockOAuth2User(Map<String, Object> attrs) {
        OAuth2User user = mock(OAuth2User.class);
        when(user.getAttributes()).thenReturn(attrs);
        return user;
    }

    @Test
    @DisplayName("detectProvider identifies GitHub from attributes")
    void detectProvider_github() {
        UserRepository repo = mock(UserRepository.class);
        OAuthUserService service = new OAuthUserService(repo);

        OAuth2User oauthUser = mockOAuth2User(Map.of(
                "login", "octocat",
                "avatar_url", "https://example.com/avatar.png"
        ));

        String provider = service.detectProvider(oauthUser);
        assertThat(provider).isEqualTo("github");
    }

    @Test
    @DisplayName("detectProvider identifies Google from attributes")
    void detectProvider_google() {
        UserRepository repo = mock(UserRepository.class);
        OAuthUserService service = new OAuthUserService(repo);

        OAuth2User oauthUser = mockOAuth2User(Map.of(
                "sub", "123456",
                "email", "user@gmail.com"
        ));

        String provider = service.detectProvider(oauthUser);
        assertThat(provider).isEqualTo("google");
    }

    @Test
    @DisplayName("detectProvider identifies Discord from attributes")
    void detectProvider_discord() {
        UserRepository repo = mock(UserRepository.class);
        OAuthUserService service = new OAuthUserService(repo);

        OAuth2User oauthUser = mockOAuth2User(Map.of(
                "username", "discordUser",
                "id", "999"
        ));

        String provider = service.detectProvider(oauthUser);
        assertThat(provider).isEqualTo("discord");
    }

    @Test
    @DisplayName("getOrCreateUser updates existing user by provider + id")
    void getOrCreateUser_existing() {
        UserRepository repo = mock(UserRepository.class);
        OAuthUserService service = new OAuthUserService(repo);

        OAuth2User oauthUser = mockOAuth2User(Map.of(
                "id", 123L,
                "email", "updated@example.com",
                "name", "Updated Name"
        ));

        User existing = new User();
        existing.setEmail("old@example.com");
        existing.setName("Old Name");
        existing.setOauthProvider("github");
        existing.setOauthProviderId("123");

        when(repo.findByOauthProviderAndOauthProviderId("github", "123"))
                .thenReturn(Optional.of(existing));
        when(repo.findByEmail("updated@example.com"))
                .thenReturn(Optional.of(existing)); // same user, allowed

        User result = service.getOrCreateUser(oauthUser, "github");

        assertThat(result).isSameAs(existing);
        assertThat(result.getEmail()).isEqualTo("updated@example.com");
        assertThat(result.getName()).isEqualTo("Updated Name");
    }

    @Test
    @DisplayName("getOrCreateUser creates new user when none found")
    void getOrCreateUser_newUser() {
        UserRepository repo = mock(UserRepository.class);
        OAuthUserService service = new OAuthUserService(repo);

        OAuth2User oauthUser = mockOAuth2User(Map.of(
                "id", 123L,
                "email", "new@example.com",
                "name", "New User",
                "avatar_url", "https://example.com/avatar.png"
        ));

        when(repo.findByOauthProviderAndOauthProviderId("github", "123"))
                .thenReturn(Optional.empty());
        when(repo.findByEmail("new@example.com")).thenReturn(Optional.empty());

        when(repo.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            try {
                var idField = User.class.getDeclaredField("id");
                idField.setAccessible(true);
                idField.set(u, 1L);
            } catch (Exception ignored) {}
            return u;
        });

        User user = service.getOrCreateUser(oauthUser, "github");

        assertThat(user.getId()).isEqualTo(1L);
        assertThat(user.getEmail()).isEqualTo("new@example.com");
        assertThat(user.getOauthProvider()).isEqualTo("github");
        assertThat(user.getOauthProviderId()).isEqualTo("123");
    }
}
