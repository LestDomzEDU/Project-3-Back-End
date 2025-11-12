package com.project03.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.util.StringUtils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Adds support for custom query params:
 *  - fresh=1  -> forces Google account chooser/consent
 *  - return_to=<deeplink> -> we persist it in a short-lived cookie for use on /oauth2/final
 */
public class GoogleAuthRequestResolver implements OAuth2AuthorizationRequestResolver {

  private static final String COOKIE_RETURN_TO = "oauth_return_to";
  private static final int COOKIE_MAX_AGE_SEC = 180; // 3 minutes

  private final DefaultOAuth2AuthorizationRequestResolver delegate;
  private final HttpServletResponse response;

  public GoogleAuthRequestResolver(ClientRegistrationRepository repo, String baseUri, HttpServletResponse response) {
    this.delegate = new DefaultOAuth2AuthorizationRequestResolver(repo, baseUri);
    this.response = response;
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

    // Handle fresh=1 -> prompt=consent for Google so user can pick accounts
    String fresh = request.getParameter("fresh");
    if ("1".equals(fresh) && "google".equalsIgnoreCase(req.getClientRegistrationId())) {
      addl.put("prompt", "consent");
    }

    // Persist return_to in a short-lived cookie so /oauth2/final can read it
    String returnTo = request.getParameter("return_to");
    if (StringUtils.hasText(returnTo)) {
      String enc = URLEncoder.encode(returnTo, StandardCharsets.UTF_8);
      Cookie c = new Cookie(COOKIE_RETURN_TO, enc);
      c.setHttpOnly(true);
      c.setSecure(true);
      c.setPath("/");       // visible to /oauth2/final
      c.setMaxAge(COOKIE_MAX_AGE_SEC);
      response.addCookie(c);
    }

    return OAuth2AuthorizationRequest
        .from(req)
        .additionalParameters(addl)
        .build();
  }
}
