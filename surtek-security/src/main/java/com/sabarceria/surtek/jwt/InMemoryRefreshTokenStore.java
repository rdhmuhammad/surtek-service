package com.sabarceria.surtek.jwt;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Single-node, non-persistent {@link RefreshTokenStore}.
 * <p>
 * Tokens are lost on restart and are not shared between instances, so use it only for
 * development, tests, or a single-instance deployment that accepts forced re-login on restart.
 */
public class InMemoryRefreshTokenStore implements RefreshTokenStore {

    private enum State { ACTIVE, ROTATED, REVOKED }

    private record Entry(String subject, Instant expiresAt, State state) {
        Entry with(State newState) {
            return new Entry(subject, expiresAt, newState);
        }
    }

    private final Map<String, Entry> tokens = new ConcurrentHashMap<>();

    private final Clock clock;

    public InMemoryRefreshTokenStore() {
        this(Clock.systemUTC());
    }

    public InMemoryRefreshTokenStore(Clock clock) {
        this.clock = clock;
    }

    @Override
    public void save(String jti, String subject, Instant expiresAt) {
        purgeExpired();
        tokens.put(jti, new Entry(subject, expiresAt, State.ACTIVE));
    }

    @Override
    public ConsumeResult consume(String jti) {
        ConsumeResult[] result = {ConsumeResult.UNKNOWN};
        tokens.computeIfPresent(jti, (k, entry) -> {
            switch (entry.state()) {
                case ACTIVE -> {
                    result[0] = ConsumeResult.CONSUMED;
                    return entry.with(State.ROTATED);
                }
                case ROTATED -> result[0] = ConsumeResult.REUSED;
                case REVOKED -> result[0] = ConsumeResult.REVOKED;
            }
            return entry;
        });
        return result[0];
    }

    @Override
    public void revoke(String jti) {
        tokens.computeIfPresent(jti, (k, entry) -> entry.with(State.REVOKED));
    }

    @Override
    public void revokeAllForSubject(String subject) {
        tokens.replaceAll((k, entry) -> entry.subject().equals(subject) ? entry.with(State.REVOKED) : entry);
    }

    private void purgeExpired() {
        Instant now = clock.instant();
        tokens.values().removeIf(entry -> entry.expiresAt().isBefore(now));
    }
}
