package com.sabarceria.surtek.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.WeakKeyException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Issues, verifies, rotates and revokes JWT access / refresh tokens.
 * <ul>
 *     <li>Access and refresh tokens are signed with <b>different</b> HMAC keys and carry a
 *     {@value #CLAIM_TOKEN_USE} claim, so one can never be used in place of the other.</li>
 *     <li>Every token is verified for signature, issuer, expiry and not-before (with clock skew).</li>
 *     <li>Refresh tokens are single-use: each refresh rotates the pair. Presenting an already
 *     rotated refresh token is treated as theft and revokes all of the subject's refresh tokens
 *     (OAuth 2.0 Security BCP, refresh token rotation with reuse detection).</li>
 * </ul>
 */
@Slf4j
public class JwtTokenService {

    public static final String CLAIM_TOKEN_USE = "token_use";
    public static final String CLAIM_ROLES = "roles";
    public static final String TOKEN_USE_ACCESS = "access";
    public static final String TOKEN_USE_REFRESH = "refresh";

    private static final int MIN_KEY_BYTES = 32; // 256 bits, required for HS256

    private final JwtProperties properties;
    private final RefreshTokenStore refreshTokenStore;
    private final UserDetailsService userDetailsService;
    private final Clock clock;

    private final SecretKey accessKey;
    private final SecretKey refreshKey;
    private final JwtParser accessParser;
    private final JwtParser refreshParser;

    public JwtTokenService(JwtProperties properties,
                           RefreshTokenStore refreshTokenStore,
                           UserDetailsService userDetailsService,
                           Clock clock) {
        this.properties = properties;
        this.refreshTokenStore = refreshTokenStore;
        this.userDetailsService = userDetailsService;
        this.clock = clock;

        validate(properties);
        this.accessKey = toKey("access-token-secret", properties.getAccessTokenSecret());
        this.refreshKey = toKey("refresh-token-secret", properties.getRefreshTokenSecret());
        this.accessParser = parser(accessKey, TOKEN_USE_ACCESS);
        this.refreshParser = parser(refreshKey, TOKEN_USE_REFRESH);
    }

    // ------------------------------------------------------------------ issue

    /**
     * Issue a fresh access/refresh pair, e.g. after a successful login.
     */
    public TokenPair issue(UserDetails user) {
        Instant now = clock.instant();
        Duration accessTtl = properties.getAccessTokenTtl();
        Duration refreshTtl = properties.getRefreshTokenTtl();

        List<String> roles = user.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();

        String accessToken = Jwts.builder()
                .id(UUID.randomUUID().toString())
                .issuer(properties.getIssuer())
                .subject(user.getUsername())
                .issuedAt(Date.from(now))
                .notBefore(Date.from(now))
                .expiration(Date.from(now.plus(accessTtl)))
                .claim(CLAIM_TOKEN_USE, TOKEN_USE_ACCESS)
                .claim(CLAIM_ROLES, roles)
                .signWith(accessKey)
                .compact();

        String refreshJti = UUID.randomUUID().toString();
        Instant refreshExpiresAt = now.plus(refreshTtl);
        String refreshToken = Jwts.builder()
                .id(refreshJti)
                .issuer(properties.getIssuer())
                .subject(user.getUsername())
                .issuedAt(Date.from(now))
                .notBefore(Date.from(now))
                .expiration(Date.from(refreshExpiresAt))
                .claim(CLAIM_TOKEN_USE, TOKEN_USE_REFRESH)
                .signWith(refreshKey)
                .compact();

        refreshTokenStore.save(refreshJti, user.getUsername(), refreshExpiresAt);

        return new TokenPair(accessToken, refreshToken, "Bearer", accessTtl.toSeconds(), refreshTtl.toSeconds());
    }

    // ----------------------------------------------------------------- verify

    /**
     * Verify an access token.
     *
     * @throws CredentialsExpiredException if expired (client should refresh)
     * @throws BadCredentialsException     for any other invalid token
     */
    public Claims parseAccessToken(String token) {
        return parse(accessParser, token, "access");
    }

    /**
     * Verify a refresh token's signature and claims (does not consume it).
     */
    public Claims parseRefreshToken(String token) {
        return parse(refreshParser, token, "refresh");
    }

    // ----------------------------------------------------------------- rotate

    /**
     * Exchange a valid refresh token for a new pair. The presented token becomes unusable.
     *
     * @throws org.springframework.security.core.AuthenticationException if the token is invalid,
     *                                                                   reused, revoked, or the user is no longer allowed in
     */
    public TokenPair refresh(String refreshToken) {
        Claims claims = parseRefreshToken(refreshToken);
        String subject = claims.getSubject();

        switch (refreshTokenStore.consume(claims.getId())) {
            case CONSUMED -> { /* ok */ }
            case REUSED -> {
                log.warn("SECURITY: refresh token reuse detected for subject '{}' (jti={}); revoking all sessions",
                        subject, claims.getId());
                refreshTokenStore.revokeAllForSubject(subject);
                throw new BadCredentialsException("Refresh token has already been used");
            }
            case REVOKED, UNKNOWN -> throw new BadCredentialsException("Refresh token is no longer valid");
        }

        UserDetails user = loadUser(subject);
        if (!user.isEnabled() || !user.isAccountNonExpired() || !user.isAccountNonLocked()) {
            refreshTokenStore.revokeAllForSubject(subject);
            throw new DisabledException("User account is not active");
        }
        return issue(user);
    }

    // ----------------------------------------------------------------- revoke

    /**
     * Revoke a single refresh token (logout on this device). Invalid tokens are ignored.
     */
    public void revoke(String refreshToken) {
        try {
            refreshTokenStore.revoke(parseRefreshToken(refreshToken).getId());
        } catch (BadCredentialsException | CredentialsExpiredException ignored) {
            // already unusable
        }
    }

    /**
     * Revoke every refresh token of a user (logout everywhere / password change).
     */
    public void revokeAll(String subject) {
        refreshTokenStore.revokeAllForSubject(subject);
    }

    // ---------------------------------------------------------------- helpers

    private UserDetails loadUser(String subject) {
        try {
            UserDetails user = userDetailsService.loadUserByUsername(subject);
            if (user == null) {
                throw new UsernameNotFoundException(subject);
            }
            return user;
        } catch (UsernameNotFoundException e) {
            refreshTokenStore.revokeAllForSubject(subject);
            throw new BadCredentialsException("User no longer exists", e);
        }
    }

    private static Claims parse(JwtParser parser, String token, String kind) {
        if (!StringUtils.hasText(token)) {
            throw new BadCredentialsException("Missing " + kind + " token");
        }
        try {
            return parser.parseSignedClaims(token).getPayload();
        } catch (ExpiredJwtException e) {
            throw new CredentialsExpiredException(capitalize(kind) + " token has expired", e);
        } catch (JwtException | IllegalArgumentException e) {
            // Do not leak the precise reason (bad signature vs. wrong issuer...) to the client.
            log.debug("Rejected {} token: {}", kind, e.getMessage());
            throw new BadCredentialsException("Invalid " + kind + " token", e);
        }
    }

    private JwtParser parser(SecretKey key, String tokenUse) {
        return Jwts.parser()
                .verifyWith(key)
                .requireIssuer(properties.getIssuer())
                .require(CLAIM_TOKEN_USE, tokenUse)
                .clockSkewSeconds(properties.getClockSkew().toSeconds())
                .clock(() -> Date.from(clock.instant()))
                .build();
    }

    private static void validate(JwtProperties p) {
        requireText("issuer", p.getIssuer());
        requireText("access-token-secret", p.getAccessTokenSecret());
        requireText("refresh-token-secret", p.getRefreshTokenSecret());
        if (p.getAccessTokenSecret().equals(p.getRefreshTokenSecret())) {
            throw new IllegalStateException("surtek.security.jwt.access-token-secret and refresh-token-secret must be different keys");
        }
        if (p.getAccessTokenTtl().isNegative() || p.getAccessTokenTtl().isZero()
                || p.getRefreshTokenTtl().compareTo(p.getAccessTokenTtl()) <= 0) {
            throw new IllegalStateException("surtek.security.jwt: require 0 < access-token-ttl < refresh-token-ttl");
        }
    }

    private static SecretKey toKey(String name, String base64Secret) {
        byte[] bytes;
        try {
            bytes = Decoders.BASE64.decode(base64Secret);
        } catch (RuntimeException e) {
            throw new IllegalStateException("surtek.security.jwt." + name + " must be base64 encoded", e);
        }
        if (bytes.length < MIN_KEY_BYTES) {
            throw new IllegalStateException("surtek.security.jwt." + name + " must decode to at least "
                    + MIN_KEY_BYTES + " bytes (got " + bytes.length + ")");
        }
        try {
            return Keys.hmacShaKeyFor(bytes);
        } catch (WeakKeyException e) {
            throw new IllegalStateException("surtek.security.jwt." + name + " is too weak", e);
        } finally {
            Arrays.fill(bytes, (byte) 0);
        }
    }

    private static void requireText(String name, String value) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalStateException("Missing required property surtek.security.jwt." + name);
        }
    }

    private static String capitalize(String s) {
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
