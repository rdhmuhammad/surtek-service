package com.sabarceria.surtek.jwt;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * JWT settings, bound from {@code surtek.security.jwt.*}.
 *
 * <pre>
 * surtek:
 *   security:
 *     jwt:
 *       issuer: surtek-api
 *       access-token-secret: ${JWT_ACCESS_SECRET}    # base64, >= 32 bytes decoded
 *       refresh-token-secret: ${JWT_REFRESH_SECRET}  # base64, >= 32 bytes decoded, different key
 *       access-token-ttl: 15m
 *       refresh-token-ttl: 7d
 *       clock-skew: 30s
 *       refresh-path: /auth/refresh
 * </pre>
 * Generate a secret with e.g. {@code openssl rand -base64 64}. Never commit real secrets.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "surtek.security.jwt")
public class JwtProperties {

    /** Value of the {@code iss} claim; tokens with a different issuer are rejected. */
    private String issuer = "surtek";

    /** Base64-encoded HMAC key for access tokens. */
    private String accessTokenSecret;

    /** Base64-encoded HMAC key for refresh tokens. Must differ from the access key. */
    private String refreshTokenSecret;

    private Duration accessTokenTtl = Duration.ofMinutes(15);

    private Duration refreshTokenTtl = Duration.ofDays(7);

    /** Tolerated clock difference between token issuer and this server. */
    private Duration clockSkew = Duration.ofSeconds(30);

    /** Endpoint (POST) handled by {@link com.sabarceria.surtek.filter.RefreshTokenFilter}. */
    private String refreshPath = "/auth/refresh";
}
