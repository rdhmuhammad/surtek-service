package com.sabarceria.surtek.oauth2;

import java.util.Map;

/**
 * Creates provider-specific {@link OAuth2UserInfo} for supported providers (Google, GitHub, Discord).
 */
public final class OAuth2UserInfoFactory {

    private OAuth2UserInfoFactory() {
    }

    public static OAuth2UserInfo getOAuth2UserInfo(String registrationId, Map<String, Object> attributes) {
        if ("google".equalsIgnoreCase(registrationId)) {
            return new GoogleOAuth2UserInfo(attributes);
        } else if ("github".equalsIgnoreCase(registrationId)) {
            return new GithubOAuth2UserInfo(attributes);
        } else if ("discord".equalsIgnoreCase(registrationId)) {
            return new DiscordOAuth2UserInfo(attributes);
        }
        throw new IllegalArgumentException("Unsupported OAuth2 provider: " + registrationId);
    }
}
