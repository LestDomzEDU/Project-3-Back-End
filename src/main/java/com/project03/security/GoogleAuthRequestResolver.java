package com.project03.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Minimal resolver that preserves our "fresh=1" flag for Google by setting prompt=consent.
 * (We store return_to in a cookie using ReturnToCookieFilter, not here.)
 */
public class GoogleAuthRequestResolver implements OAuth2AuthorizationRequestResolver {

  private final DefaultOAuth2AuthorizationRequestResolver delegate;

  public GoogleAuthRequestResolver(ClientRegistrationRepository repo, String baseUri) {
    this.delegate = new DefaultOAuth2AuthorizationRequestResolver(repo, baseUri);
  }

  @Override
  public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
    OAuth2AuthorizationRequest req = delegate.resolve(request);
    return mutate(request, req);
  }

  @Override
  public OAuth2AuthorizationRequest resolve(HttpServletRequest request, String clientRegistrationId) {
    OAuth2AuthorizationRequest req = delegate.resolve(request, clientRegistrationId);
    return mutate(request, req);
  }

  private OAuth2AuthorizationRequest mutate(HttpServletRequest request, OAuth2AuthorizationRequest req) {
    if (req == null) return null;

    Map<String, Object> addl = new LinkedHashMap<>(req.getAdditionalParameters());

    // If caller passed fresh=1 and provider is google, force the account chooser
    String fresh = request.getParameter("fresh");
    if ("google".equalsIgnoreCase(req.getClientRegistrationId()) && "1".equals(fresh)) {
      addl.put("prompt", "consent");
    }

    return OAuth2AuthorizationRequest
        .from(req)
        .additionalParameters(addl)
        .build();
  }
}
