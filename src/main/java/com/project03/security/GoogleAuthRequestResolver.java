package com.project03.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Minimal resolver that:
 *  - preserves "fresh=1" by mapping it to prompt=consent for Google
 *  - otherwise delegates to the default resolver
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

    // Determine the registration id ("google", "github", etc.)
    String regId = null;
    Object attr = req.getAttribute(OAuth2ParameterNames.REGISTRATION_ID);
    if (attr != null) regId = String.valueOf(attr);

    // Fallback: parse from URI .../oauth2/authorization/{registrationId}
    if (regId == null) {
      String uri = request.getRequestURI();
      if (uri != null) {
        int idx = uri.lastIndexOf('/');
        if (idx >= 0 && idx + 1 < uri.length()) regId = uri.substring(idx + 1);
      }
    }

    Map<String, Object> addl = new LinkedHashMap<>(req.getAdditionalParameters());

    // If caller asked for a "fresh" Google login, force the account chooser
    String fresh = request.getParameter("fresh");
    if ("google".equalsIgnoreCase(regId) && "1".equals(fresh)) {
      addl.put("prompt", "consent");
    }

    return OAuth2AuthorizationRequest
        .from(req)
        .additionalParameters(addl)
        .build();
  }
}
