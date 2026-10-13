package com.sabarceria.surtek.filter;

import com.sabarceria.surtek.config.ModuleSpecificAuth;
import com.sabarceria.surtek.exception.AccessNotAllowException;
import com.sabarceria.surtek.jwt.JwtTokenService;
import com.sabarceria.surtek.model.UserDetailsImpl;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * Authenticates every request with a JWT access token ({@code Authorization: Bearer <token>}).
 * <p>
 * Requests without a valid token are rejected with 401. Make an endpoint public with
 * {@code @BypassFilter(JwtAuthenticationFilter.class)}.
 * <p>
 * On success the user is (re)loaded through {@link UserDetailsService} so that disabled or deleted
 * users lose access immediately, and the {@link SecurityContextHolder} is populated so that
 * {@code @RolesAllowed} works.
 */
@Slf4j
@Component
@Order(JwtAuthenticationFilter.ORDER)
public class JwtAuthenticationFilter extends BaseFilter {

    public static final int ORDER = 200;

    /** Request attribute holding the authenticated {@link com.sabarceria.surtek.model.ClientManifest}. */
    public static final String CLIENT_MANIFEST_ATTRIBUTE = "CLIENT_MANIFEST";

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenService jwtTokenService;
    private final UserDetailsService userDetailsService;
    private final ObjectProvider<ModuleSpecificAuth> moduleSpecificAuth;
    private final boolean useModuleAuthenticated;

    public JwtAuthenticationFilter(JwtTokenService jwtTokenService,
                                   UserDetailsService userDetailsService,
                                   ObjectProvider<ModuleSpecificAuth> moduleSpecificAuth,
                                   @Value("${security.auth.use-module-authenticated:false}") boolean useModuleAuthenticated) {
        this.jwtTokenService = jwtTokenService;
        this.userDetailsService = userDetailsService;
        this.moduleSpecificAuth = moduleSpecificAuth;
        this.useModuleAuthenticated = useModuleAuthenticated;
    }

    @Override
    protected void onFilterChain(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Claims claims = jwtTokenService.parseAccessToken(resolveBearerToken(request));

        UserDetails user = loadUser(claims.getSubject());
        if (!user.isEnabled() || !user.isAccountNonExpired() || !user.isAccountNonLocked()) {
            log.warn("User {} status is invalid", user.getUsername());
            throw new DisabledException("User account is not active");
        }

        if (user instanceof UserDetailsImpl details && details.getUserEntity() != null) {
            request.setAttribute(CLIENT_MANIFEST_ATTRIBUTE, details.getUserEntity());
        }
        authenticate(request, user);
        enforceModuleAccess(request, user.getUsername());

        chain.doFilter(request, response);
    }

    private static String resolveBearerToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        // Auth scheme is case-insensitive (RFC 7235 §2.1).
        if (header == null || !header.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            throw new BadCredentialsException("Missing bearer token");
        }
        String token = header.substring(BEARER_PREFIX.length()).trim();
        if (token.isEmpty()) {
            throw new BadCredentialsException("Missing bearer token");
        }
        return token;
    }

    private UserDetails loadUser(String username) {
        try {
            UserDetails user = userDetailsService.loadUserByUsername(username);
            if (user == null) {
                throw new UsernameNotFoundException(username);
            }
            return user;
        } catch (UsernameNotFoundException e) {
            log.warn("User identity not found: {}", username);
            throw new BadCredentialsException("Invalid access token", e);
        }
    }

    private static void authenticate(HttpServletRequest request, UserDetails user) {
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
    }

    /**
     * When {@code security.auth.use-module-authenticated=true} and a {@link ModuleSpecificAuth}
     * whitelist is configured, the user may only call the endpoints listed for them.
     */
    private void enforceModuleAccess(HttpServletRequest request, String username) {
        if (!useModuleAuthenticated) {
            return;
        }
        ModuleSpecificAuth modules = moduleSpecificAuth.getIfAvailable();
        if (modules == null || modules.getModules() == null || modules.getModules().isEmpty()) {
            return;
        }
        List<String> endpoints = modules.getEndpoints(username);
        boolean allowed = endpoints.stream()
                .filter(StringUtils::hasText)
                .anyMatch(endpoint -> new AntPathRequestMatcher(endpoint).matches(request));
        if (!allowed) {
            throw new AccessNotAllowException(username);
        }
    }
}
