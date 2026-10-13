package com.sabarceria.surtek.jwt;

import java.time.Instant;

/**
 * Server-side state of issued refresh tokens, keyed by the token's {@code jti}.
 * <p>
 * Required for rotation and revocation: a signed JWT alone cannot be invalidated before it
 * expires. Provide your own bean (Redis / database) for production and multi-instance
 * deployments; {@link InMemoryRefreshTokenStore} is used only as a fallback.
 * <p>
 * Implementations must be thread-safe, and {@link #consume(String)} must be atomic.
 */
public interface RefreshTokenStore {

    /** Record a newly issued refresh token as active. */
    void save(String jti, String subject, Instant expiresAt);

    /**
     * Atomically move an active token to "rotated" so it can be used only once.
     */
    ConsumeResult consume(String jti);

    /** Revoke one token (e.g. logout from one device). */
    void revoke(String jti);

    /** Revoke every token of a subject (e.g. logout everywhere, password change, theft detected). */
    void revokeAllForSubject(String subject);

    enum ConsumeResult {
        /** Token was active and is now rotated: issue a new pair. */
        CONSUMED,
        /** Token was already rotated: it is being replayed, treat as stolen. */
        REUSED,
        /** Token was revoked. */
        REVOKED,
        /** Token was never issued by this server, or has been purged after expiry. */
        UNKNOWN
    }
}
