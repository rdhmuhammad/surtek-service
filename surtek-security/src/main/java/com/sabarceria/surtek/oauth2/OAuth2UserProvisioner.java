package com.sabarceria.surtek.oauth2;

import org.springframework.security.core.userdetails.UserDetails;

/**
 * Strategy interface to resolve or provision a {@link UserDetails} for an authenticated OAuth2 user.
 * Can be customized by declaring a bean implementing this interface in your application.
 */
public interface OAuth2UserProvisioner {

    /**
     * Resolves an existing user or provisions a new one based on OAuth2 user profile information.
     *
     * @param registrationId "google", "github", or "discord"
     * @param userInfo       the normalized user profile
     * @return the resolved or provisioned UserDetails ready for JWT issuance
     */
    UserDetails loadOrCreateUser(String registrationId, OAuth2UserInfo userInfo);
}
