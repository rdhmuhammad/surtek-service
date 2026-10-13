package com.sabarceria.surtek.filter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sabarceria.surtek.jwt.JwtProperties;
import com.sabarceria.surtek.jwt.JwtTokenService;
import com.sabarceria.surtek.jwt.TokenPair;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Token refresh endpoint implemented as a filter.
 * <p>
 * {@code POST {surtek.security.jwt.refresh-path}} (default {@code /auth/refresh}) with body
 * {@code {"refresh_token": "..."}} returns a new {@link TokenPair}. The presented refresh token
 * is consumed (rotation); replaying it revokes all of the user's refresh tokens.
 * <p>
 * Runs before {@link JwtAuthenticationFilter} and answers the request itself, so an expired
 * access token does not block refreshing. All other requests pass straight through.
 */
@Slf4j
@Component
@Order(RefreshTokenFilter.ORDER)
public class RefreshTokenFilter extends BaseFilter {

    public static final int ORDER = 100;

    private static final int MAX_BODY_BYTES = 8 * 1024;

    private final JwtTokenService jwtTokenService;
    private final ObjectMapper objectMapper;
    private final RequestMatcher refreshEndpoint;

    public RefreshTokenFilter(JwtTokenService jwtTokenService, ObjectMapper objectMapper, JwtProperties properties) {
        this.jwtTokenService = jwtTokenService;
        this.objectMapper = objectMapper;
        this.refreshEndpoint = new AntPathRequestMatcher(properties.getRefreshPath(), "POST");
    }

    @Override
    protected void onFilterChain(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (!refreshEndpoint.matches(request)) {
            chain.doFilter(request, response);
            return;
        }

        TokenPair tokens = jwtTokenService.refresh(readRefreshToken(request));

        response.setStatus(HttpStatus.OK.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        // Token responses must never be cached (RFC 6749 §5.1).
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        response.setHeader(HttpHeaders.PRAGMA, "no-cache");
        objectMapper.writeValue(response.getOutputStream(), tokens);
    }

    private String readRefreshToken(HttpServletRequest request) throws IOException {
        byte[] body = request.getInputStream().readNBytes(MAX_BODY_BYTES + 1);
        if (body.length > MAX_BODY_BYTES) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "Request body too large");
        }
        JsonNode json;
        try {
            json = body.length == 0 ? null : objectMapper.readTree(body);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Malformed JSON body");
        }
        JsonNode token = json == null ? null : json.get("refresh_token");
        if (token == null || !token.isTextual() || token.asText().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "refresh_token is required");
        }
        return token.asText();
    }
}
