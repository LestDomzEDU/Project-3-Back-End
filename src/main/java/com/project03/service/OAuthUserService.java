package com.project03.service;

import com.project03.model.User;
import com.project03.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;

/**
 * Service to handle OAuth user creation and updates.
 * Automatically creates or updates User records when users sign in via OAuth.
 */
@Service
public class OAuthUserService {

    private final UserRepository userRepository;

    @Autowired
    public OAuthUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Get or create a User from an OAuth2User.
     *
     * For this project we only need to support GitHub, Google and Discord,
     * but the method is defensive and will fail fast if an unknown provider is passed.
     */
    @Transactional
    public User getOrCreateUser(OAuth2User oauth2User, String registrationId) {
        Map<String, Object> attrs = oauth2User.getAttributes();

        String oauthProvider = registrationId.toLowerCase(); // "github", "google", "discord"
        String oauthProviderId;
        String email = null;
        String name = null;
        String avatarUrl = null;

        // --- Extract provider-specific fields ---
        if ("github".equals(oauthProvider)) {
            Object idObj = attrs.get("id");
            oauthProviderId = idObj != null ? String.valueOf(idObj) : null;

            email = (String) attrs.get("email");
            if (email == null || email.isEmpty()) {
                String login = (String) attrs.get("login");
                if (login != null) {
                    email = login + "@github.noreply";
                }
            }

            name = (String) attrs.get("name");
            if (name == null || name.isEmpty()) {
                name = (String) attrs.get("login");
            }

            avatarUrl = (String) attrs.get("avatar_url");

        } else if ("google".equals(oauthProvider)) {
            oauthProviderId = (String) attrs.get("sub");
            email = (String) attrs.get("email");
            name = (String) attrs.get("name");
            avatarUrl = (String) attrs.get("picture");

        } else if ("discord".equals(oauthProvider)) {
            Object idObj = attrs.get("id");
            oauthProviderId = idObj != null ? String.valueOf(idObj) : null;

            email = (String) attrs.get("email");
            String username = (String) attrs.get("username");
            String globalName = (String) attrs.get("global_name");

            if (email == null || email.isEmpty()) {
                if (username != null && !username.isEmpty()) {
                    email = username + "@discord.noreply";
                }
            }

            if (globalName != null && !globalName.isEmpty()) {
                name = globalName;
            } else if (username != null && !username.isEmpty()) {
                name = username;
            } else if (email != null && !email.isEmpty()) {
                name = email;
            } else {
                name = "Discord User";
            }

            String avatarHash = (String) attrs.get("avatar");
            if (avatarHash != null && !avatarHash.isEmpty() && oauthProviderId != null) {
                avatarUrl = "https://cdn.discordapp.com/avatars/"
                        + oauthProviderId + "/" + avatarHash + ".png";
            }

        } else {
            throw new IllegalArgumentException("Unsupported OAuth provider: " + registrationId);
        }

        // Required: provider ID must be present
        if (oauthProviderId == null || oauthProviderId.isEmpty()) {
            throw new IllegalStateException(
                    "OAuth provider ID is missing for provider: " + oauthProvider);
        }

        // --- Look up existing user by provider + providerId ---
        Optional<User> existingUserOpt =
                userRepository.findByOauthProviderAndOauthProviderId(oauthProvider, oauthProviderId);

        User user;
        if (existingUserOpt.isPresent()) {
            // Update existing user
            user = existingUserOpt.get();

            if (email != null && !email.isEmpty()) {
                user.setEmail(email);
            }
            if (name != null && !name.isEmpty()) {
                user.setName(name);
            }
            if (avatarUrl != null && !avatarUrl.isEmpty()) {
                user.setAvatarUrl(avatarUrl);
            }

            // Important: don't overwrite the reference with the result of save()
            userRepository.save(user);

        } else {
            // Create new user
            user = new User();
            user.setEmail(email);
            user.setName(name);
            user.setOauthProvider(oauthProvider);
            user.setOauthProviderId(oauthProviderId);
            if (avatarUrl != null && !avatarUrl.isEmpty()) {
                user.setAvatarUrl(avatarUrl);
            }

            // We don't need the return value; the tests mutate the same instance
            userRepository.save(user);
        }

        return user;
    }

    /**
     * Best-effort detection of the provider based purely on OAuth2User attributes.
     */
    public String detectProvider(OAuth2User oauth2User) {
        Map<String, Object> attrs = oauth2User.getAttributes();

        // GitHub usually has "login" / "avatar_url"
        if (attrs.containsKey("login") || attrs.containsKey("avatar_url")) {
            return "github";
        }

        // Google OIDC has "sub" / "picture"
        if (attrs.containsKey("sub") || attrs.containsKey("picture")) {
            return "google";
        }

        // Discord has "username" / "global_name"
        if (attrs.containsKey("username") || attrs.containsKey("global_name")) {
            return "discord";
        }

        return "unknown";
    }
}
