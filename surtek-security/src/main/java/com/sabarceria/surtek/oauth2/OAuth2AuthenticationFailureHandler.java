package com.sabarceria.surtek.oauth2;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static com.sabarceria.surtek.oauth2.HttpCookieOAuth2AuthorizationRequestRepository.REDIRECT_URI_PARAM_COOKIE_NAME;

/**
 * Handles failed OAuth2 authentication attempts.
 */
@Slf4j
public class OAuth2AuthenticationFailureHandler implements AuthenticationFailureHandler {

    private final OAuth2Properties properties;
    private final HttpCookieOAuth2AuthorizationRequestRepository cookieRepository;

    public OAuth2AuthenticationFailureHandler(OAuth2Properties properties,
                                              HttpCookieOAuth2AuthorizationRequestRepository cookieRepository) {
        this.properties = properties;
        this.cookieRepository = cookieRepository;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception)
            throws IOException, ServletException {
        log.warn("OAuth2 authentication failure: {}", exception.getMessage());

        String redirectUri = cookieRepository.getCookie(request, REDIRECT_URI_PARAM_COOKIE_NAME)
                .map(Cookie::getValue)
                .filter(StringUtils::hasText)
                .orElse(properties.getRedirectUri());

        cookieRepository.removeAuthorizationRequestCookies(request, response);

        if (StringUtils.hasText(redirectUri)) {
            String targetUrl = UriComponentsBuilder.fromUriString(redirectUri)
                    .queryParam("error", exception.getLocalizedMessage())
                    .build().toUriString();
            response.sendRedirect(targetUrl);
        } else {
            response.sendError(HttpStatus.UNAUTHORIZED.value(), exception.getMessage());
        }
    }
}
