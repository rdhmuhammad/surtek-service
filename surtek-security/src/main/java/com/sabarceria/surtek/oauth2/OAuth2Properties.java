package com.sabarceria.surtek.oauth2;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * OAuth2 integration settings, bound from {@code surtek.security.oauth2.*}.
 *
 * <pre>
 * surtek:
 *   security:
 *     oauth2:
 *       redirect-uri: http://localhost:3000/oauth2/callback # Frontend redirect after OAuth success
 *       authorized-redirect-uris:
 *         - http://localhost:3000/oauth2/callback
 *       cookie-expire-seconds: 180
 * </pre>
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "surtek.security.oauth2")
public class OAuth2Properties {

    /** Default frontend redirect URL after OAuth2 authentication success (null for JSON TokenPair response). */
    private String redirectUri;

    /** Authorized redirect URIs for clients passing ?redirect_uri=... during OAuth initiation. */
    private List<String> authorizedRedirectUris = new ArrayList<>();

    /** Lifetime of the authorization request cookie in seconds. */
    private int cookieExpireSeconds = 180;
}
