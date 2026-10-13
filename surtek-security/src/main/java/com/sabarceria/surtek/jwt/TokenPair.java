package com.sabarceria.surtek.jwt;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Token response, shaped like an OAuth 2.0 token response (RFC 6749 §5.1).
 *
 * @param expiresIn        access token lifetime in seconds
 * @param refreshExpiresIn refresh token lifetime in seconds
 */
public record TokenPair(
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("refresh_token") String refreshToken,
        @JsonProperty("token_type") String tokenType,
        @JsonProperty("expires_in") long expiresIn,
        @JsonProperty("refresh_expires_in") long refreshExpiresIn) {
}
