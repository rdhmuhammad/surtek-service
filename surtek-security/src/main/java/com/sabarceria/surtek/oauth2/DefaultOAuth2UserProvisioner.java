package com.sabarceria.surtek.oauth2;

import com.sabarceria.surtek.model.UserDetailsImpl;
import com.sabarceria.surtek.repo.BaseUserSecurityService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * Default implementation of {@link OAuth2UserProvisioner}.
 * Attempts to load the user by email or {@code <provider>:<id>}; if not found, provisions a new active user.
 */
@Slf4j
public class DefaultOAuth2UserProvisioner implements OAuth2UserProvisioner {

    private final UserDetailsService userDetailsService;
    private final ObjectProvider<BaseUserSecurityService> userSecurityService;

    public DefaultOAuth2UserProvisioner(UserDetailsService userDetailsService,
                                        ObjectProvider<BaseUserSecurityService> userSecurityService) {
        this.userDetailsService = userDetailsService;
        this.userSecurityService = userSecurityService;
    }

    @Override
    public UserDetails loadOrCreateUser(String registrationId, OAuth2UserInfo userInfo) {
        String username = resolveUsername(registrationId, userInfo);

        UserDetails user;
        try {
            user = userDetailsService.loadUserByUsername(username);
            log.info("Found existing user for OAuth2 login: {}", username);
        } catch (UsernameNotFoundException ex) {
            log.info("Provisioning new user for OAuth2 login: {} (provider: {})", username, registrationId);
            user = new UserDetailsImpl(
                    () -> username,
                    "",
                    List.of(new SimpleGrantedAuthority("ROLE_USER")),
                    true,
                    false
            );
        }

        userSecurityService.ifAvailable(service -> {
            try {
                service.authenticateUser(username);
            } catch (Exception e) {
                log.debug("authenticateUser callback note: {}", e.getMessage());
            }
        });

        return user;
    }

    protected String resolveUsername(String registrationId, OAuth2UserInfo userInfo) {
        if (StringUtils.hasText(userInfo.getEmail())) {
            return userInfo.getEmail().trim().toLowerCase();
        }
        return registrationId.toLowerCase() + ":" + userInfo.getId();
    }
}
