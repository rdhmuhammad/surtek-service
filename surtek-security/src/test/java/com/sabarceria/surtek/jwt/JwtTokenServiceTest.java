package com.sabarceria.surtek.jwt;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenServiceTest {

    private JwtProperties properties;
    private InMemoryRefreshTokenStore store;
    private UserDetailsService userDetailsService;
    private AtomicReference<Instant> now;
    private Clock mutableClock;
    private JwtTokenService service;

    @BeforeEach
    void setUp() {
        properties = new JwtProperties();
        properties.setIssuer("test-issuer");
        // 32-byte keys encoded in base64
        properties.setAccessTokenSecret(Base64.getEncoder().encodeToString("access-secret-key-32-bytes-long!".getBytes()));
        properties.setRefreshTokenSecret(Base64.getEncoder().encodeToString("refresh-secret-key-32-bytes-long".getBytes()));
        properties.setAccessTokenTtl(Duration.ofMinutes(15));
        properties.setRefreshTokenTtl(Duration.ofDays(7));
        properties.setClockSkew(Duration.ZERO);

        now = new AtomicReference<>(Instant.parse("2026-01-01T00:00:00Z"));
        mutableClock = new Clock() {
            @Override public ZoneId getZone() { return ZoneId.of("UTC"); }
            @Override public Clock withZone(ZoneId zone) { return this; }
            @Override public Instant instant() { return now.get(); }
        };

        store = new InMemoryRefreshTokenStore(mutableClock);

        UserDetails testUser = new User("admin", "password", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        userDetailsService = username -> {
            if ("admin".equals(username)) return testUser;
            return null;
        };

        service = new JwtTokenService(properties, store, userDetailsService, mutableClock);
    }

    @Test
    void testIssueAndParseAccessToken() {
        UserDetails user = new User("admin", "pwd", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        TokenPair pair = service.issue(user);

        assertNotNull(pair.accessToken());
        assertNotNull(pair.refreshToken());
        assertEquals("Bearer", pair.tokenType());

        var claims = service.parseAccessToken(pair.accessToken());
        assertEquals("admin", claims.getSubject());
        assertEquals("test-issuer", claims.getIssuer());
        assertEquals(JwtTokenService.TOKEN_USE_ACCESS, claims.get(JwtTokenService.CLAIM_TOKEN_USE));
    }

    @Test
    void testAccessTokenExpired() {
        UserDetails user = new User("admin", "pwd", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        TokenPair pair = service.issue(user);

        // Advance clock past expiration
        now.set(now.get().plus(Duration.ofMinutes(16)));

        assertThrows(CredentialsExpiredException.class, () -> service.parseAccessToken(pair.accessToken()));
    }

    @Test
    void testRefreshTokenRotation() {
        UserDetails user = new User("admin", "pwd", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        TokenPair pair = service.issue(user);

        // Advance 10 minutes
        now.set(now.get().plus(Duration.ofMinutes(10)));

        TokenPair refreshed = service.refresh(pair.refreshToken());
        assertNotNull(refreshed.accessToken());
        assertNotNull(refreshed.refreshToken());
        assertNotEquals(pair.refreshToken(), refreshed.refreshToken());

        // Replaying the old refresh token must fail with reuse detection
        assertThrows(BadCredentialsException.class, () -> service.refresh(pair.refreshToken()));

        // Since reuse detection revokes all sessions, the second refreshed token is now also revoked
        assertThrows(BadCredentialsException.class, () -> service.refresh(refreshed.refreshToken()));
    }

    @Test
    void testCannotUseRefreshTokenAsAccessToken() {
        UserDetails user = new User("admin", "pwd", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        TokenPair pair = service.issue(user);

        // Refresh token signed with different key and has token_use=refresh
        assertThrows(BadCredentialsException.class, () -> service.parseAccessToken(pair.refreshToken()));
    }
}
