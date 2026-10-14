package com.sabarceria.surtek.oauth2;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sabarceria.surtek.jwt.JwtTokenService;
import com.sabarceria.surtek.jwt.TokenPair;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.util.Optional;

import static com.sabarceria.surtek.oauth2.HttpCookieOAuth2AuthorizationRequestRepository.REDIRECT_URI_PARAM_COOKIE_NAME;

/**
 * Handles successful OAuth2 authentication by provisioning/loading the user,
 * issuing a JWT {@link TokenPair} via {@link JwtTokenService}, and redirecting to the frontend
 * with tokens in the query string or returning them directly as JSON.
 */
@Slf4j
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final JwtTokenService jwtTokenService;
    private final OAuth2UserProvisioner userProvisioner;
    private final OAuth2Properties properties;
    private final HttpCookieOAuth2AuthorizationRequestRepository cookieRepository;
    private final ObjectMapper objectMapper;

    public OAuth2AuthenticationSuccessHandler(JwtTokenService jwtTokenService,
                                              OAuth2UserProvisioner userProvisioner,
                                              OAuth2Properties properties,
                                              HttpCookieOAuth2AuthorizationRequestRepository cookieRepository,
                                              ObjectMapper objectMapper) {
        this.jwtTokenService = jwtTokenService;
        this.userProvisioner = userProvisioner;
        this.properties = properties;
        this.cookieRepository = cookieRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException, ServletException {
        if (!(authentication instanceof OAuth2AuthenticationToken oauthToken)) {
            log.error("Authentication is not an OAuth2AuthenticationToken: {}", authentication);
            response.sendError(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Invalid authentication token");
            return;
        }

        String registrationId = oauthToken.getAuthorizedClientRegistrationId();
        OAuth2User oAuth2User = oauthToken.getPrincipal();
        OAuth2UserInfo userInfo = OAuth2UserInfoFactory.getOAuth2UserInfo(registrationId, oAuth2User.getAttributes());

        UserDetails user = userProvisioner.loadOrCreateUser(registrationId, userInfo);
        TokenPair tokenPair = jwtTokenService.issue(user);

        Optional<String> targetRedirectUri = determineTargetUrl(request);
        cookieRepository.removeAuthorizationRequestCookies(request, response);

        if (targetRedirectUri.isPresent()) {
            String targetUrl = UriComponentsBuilder.fromUriString(targetRedirectUri.get())
                    .queryParam("access_token", tokenPair.accessToken())
                    .queryParam("refresh_token", tokenPair.refreshToken())
                    .queryParam("token_type", tokenPair.tokenType())
                    .queryParam("expires_in", tokenPair.expiresIn())
                    .queryParam("refresh_expires_in", tokenPair.refreshExpiresIn())
                    .build().toUriString();

            log.info("OAuth2 login succeeded for {} (provider: {}), redirecting to {}", user.getUsername(), registrationId, targetRedirectUri.get());
            response.sendRedirect(targetUrl);
        } else {
            log.info("OAuth2 login succeeded for {} (provider: {}), writing JSON response", user.getUsername(), registrationId);
            response.setStatus(HttpStatus.OK.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
            response.setHeader(HttpHeaders.PRAGMA, "no-cache");
            objectMapper.writeValue(response.getOutputStream(), tokenPair);
        }
    }

    protected Optional<String> determineTargetUrl(HttpServletRequest request) {
        Optional<String> redirectUriFromCookie = cookieRepository.getCookie(request, REDIRECT_URI_PARAM_COOKIE_NAME)
                .map(Cookie::getValue)
                .filter(StringUtils::hasText);

        if (redirectUriFromCookie.isPresent()) {
            String redirectUri = redirectUriFromCookie.get();
            if (isAuthorizedRedirectUri(redirectUri)) {
                return Optional.of(redirectUri);
            }
            log.warn("Unauthorized redirect URI requested: {}", redirectUri);
        }

        if (StringUtils.hasText(properties.getRedirectUri())) {
            return Optional.of(properties.getRedirectUri());
        }

        return Optional.empty();
    }

    private boolean isAuthorizedRedirectUri(String uri) {
        if (properties.getAuthorizedRedirectUris() == null || properties.getAuthorizedRedirectUris().isEmpty()) {
            return true;
        }
        URI clientRedirectUri = URI.create(uri);
        return properties.getAuthorizedRedirectUris().stream().anyMatch(authorizedUri -> {
            URI authorizedURI = URI.create(authorizedUri);
            return authorizedURI.getHost().equalsIgnoreCase(clientRedirectUri.getHost())
                    && authorizedURI.getPort() == clientRedirectUri.getPort();
        });
    }
}
