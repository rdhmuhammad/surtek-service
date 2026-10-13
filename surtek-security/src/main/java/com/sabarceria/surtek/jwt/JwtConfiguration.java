package com.sabarceria.surtek.jwt;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.time.Clock;

@Slf4j
@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class JwtConfiguration {

    /**
     * Uses the application's {@link RefreshTokenStore} bean if one exists, otherwise falls back
     * to {@link InMemoryRefreshTokenStore}.
     */
    @Bean
    public JwtTokenService jwtTokenService(JwtProperties properties,
                                           UserDetailsService userDetailsService,
                                           ObjectProvider<RefreshTokenStore> refreshTokenStore,
                                           ObjectProvider<Clock> clock) {
        Clock effectiveClock = clock.getIfUnique(Clock::systemUTC);
        RefreshTokenStore store = refreshTokenStore.getIfAvailable(() -> {
            log.warn("No RefreshTokenStore bean defined: using InMemoryRefreshTokenStore. "
                    + "Refresh tokens will not survive a restart and are not shared across instances.");
            return new InMemoryRefreshTokenStore(effectiveClock);
        });
        return new JwtTokenService(properties, store, userDetailsService, effectiveClock);
    }
}
