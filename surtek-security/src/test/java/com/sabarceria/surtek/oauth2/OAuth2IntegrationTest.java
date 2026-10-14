package com.sabarceria.surtek.oauth2;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sabarceria.surtek.jwt.InMemoryRefreshTokenStore;
import com.sabarceria.surtek.jwt.JwtProperties;
import com.sabarceria.surtek.jwt.JwtTokenService;
import com.sabarceria.surtek.jwt.TokenPair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.time.Clock;
import java.time.Duration;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class OAuth2IntegrationTest {

    private JwtTokenService jwtTokenService;
    private OAuth2Properties oAuth2Properties;
    private HttpCookieOAuth2AuthorizationRequestRepository cookieRepository;
    private DefaultOAuth2UserProvisioner userProvisioner;
    private OAuth2AuthenticationSuccessHandler successHandler;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        JwtProperties jwtProperties = new JwtProperties();
        jwtProperties.setIssuer("test-issuer");
        jwtProperties.setAccessTokenSecret(Base64.getEncoder().encodeToString("access-secret-key-32-bytes-long!".getBytes()));
        jwtProperties.setRefreshTokenSecret(Base64.getEncoder().encodeToString("refresh-secret-key-32-bytes-long".getBytes()));
        jwtProperties.setAccessTokenTtl(Duration.ofMinutes(15));
        jwtProperties.setRefreshTokenTtl(Duration.ofDays(7));

        UserDetailsService userDetailsService = username -> {
            if ("existing@example.com".equals(username)) {
                return new User("existing@example.com", "pwd", List.of(new SimpleGrantedAuthority("ROLE_USER")));
            }
            throw new UsernameNotFoundException("User not found: " + username);
        };

        jwtTokenService = new JwtTokenService(jwtProperties, new InMemoryRefreshTokenStore(), userDetailsService, Clock.systemUTC());
        oAuth2Properties = new OAuth2Properties();
        cookieRepository = new HttpCookieOAuth2AuthorizationRequestRepository(oAuth2Properties);
        objectMapper = new ObjectMapper();

        StaticListableBeanFactory beanFactory = new StaticListableBeanFactory();
        userProvisioner = new DefaultOAuth2UserProvisioner(userDetailsService, beanFactory.getBeanProvider(com.sabarceria.surtek.repo.BaseUserSecurityService.class));
        successHandler = new OAuth2AuthenticationSuccessHandler(
                jwtTokenService,
                userProvisioner,
                oAuth2Properties,
                cookieRepository,
                objectMapper
        );
    }

    @Test
    void testGoogleOAuth2UserInfoExtraction() {
        Map<String, Object> attributes = Map.of(
                "sub", "google-sub-123",
                "name", "Google User",
                "email", "google@example.com",
                "picture", "https://avatar.google.com/photo.jpg"
        );
        OAuth2UserInfo userInfo = OAuth2UserInfoFactory.getOAuth2UserInfo("google", attributes);
        assertEquals("google-sub-123", userInfo.getId());
        assertEquals("Google User", userInfo.getName());
        assertEquals("google@example.com", userInfo.getEmail());
        assertEquals("https://avatar.google.com/photo.jpg", userInfo.getAvatarUrl());
    }

    @Test
    void testGithubOAuth2UserInfoExtraction() {
        Map<String, Object> attributes = Map.of(
                "id", 98765432,
                "login", "octocat",
                "name", "The Octocat",
                "email", "octocat@github.com",
                "avatar_url", "https://github.com/images/error/octocat_happy.gif"
        );
        OAuth2UserInfo userInfo = OAuth2UserInfoFactory.getOAuth2UserInfo("github", attributes);
        assertEquals("98765432", userInfo.getId());
        assertEquals("The Octocat", userInfo.getName());
        assertEquals("octocat@github.com", userInfo.getEmail());
        assertEquals("https://github.com/images/error/octocat_happy.gif", userInfo.getAvatarUrl());
    }

    @Test
    void testDiscordOAuth2UserInfoExtraction() {
        Map<String, Object> attributes = Map.of(
                "id", "123456789012345678",
                "username", "discorduser",
                "global_name", "Discord Display Name",
                "email", "discord@example.com",
                "avatar", "avatarhash123"
        );
        OAuth2UserInfo userInfo = OAuth2UserInfoFactory.getOAuth2UserInfo("discord", attributes);
        assertEquals("123456789012345678", userInfo.getId());
        assertEquals("Discord Display Name", userInfo.getName());
        assertEquals("discord@example.com", userInfo.getEmail());
        assertEquals("https://cdn.discordapp.com/avatars/123456789012345678/avatarhash123.png", userInfo.getAvatarUrl());
    }

    @Test
    void testSuccessHandlerIssuesTokensDirectJson() throws Exception {
        Map<String, Object> attributes = Map.of(
                "sub", "google-sub-1",
                "name", "Existing User",
                "email", "existing@example.com"
        );
        OAuth2User oauth2User = new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority("ROLE_USER")),
                attributes,
                "sub"
        );
        OAuth2AuthenticationToken authToken = new OAuth2AuthenticationToken(
                oauth2User,
                oauth2User.getAuthorities(),
                "google"
        );

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        successHandler.onAuthenticationSuccess(request, response, authToken);

        assertEquals(200, response.getStatus());
        assertEquals("application/json", response.getContentType());
        TokenPair pair = objectMapper.readValue(response.getContentAsString(), TokenPair.class);
        assertNotNull(pair.accessToken());
        assertNotNull(pair.refreshToken());
        assertEquals("Bearer", pair.tokenType());
    }

    @Test
    void testSuccessHandlerRedirectsWithTokens() throws Exception {
        oAuth2Properties.setRedirectUri("http://localhost:3000/oauth2/callback");

        Map<String, Object> attributes = Map.of(
                "id", "discord-snowflake-456",
                "username", "newdiscorduser",
                "email", "newdiscord@example.com"
        );
        OAuth2User oauth2User = new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority("ROLE_USER")),
                attributes,
                "id"
        );
        OAuth2AuthenticationToken authToken = new OAuth2AuthenticationToken(
                oauth2User,
                oauth2User.getAuthorities(),
                "discord"
        );

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        successHandler.onAuthenticationSuccess(request, response, authToken);

        assertEquals(302, response.getStatus());
        String redirectUrl = response.getRedirectedUrl();
        assertNotNull(redirectUrl);
        assertTrue(redirectUrl.startsWith("http://localhost:3000/oauth2/callback?"));
        assertTrue(redirectUrl.contains("access_token="));
        assertTrue(redirectUrl.contains("refresh_token="));
        assertTrue(redirectUrl.contains("token_type=Bearer"));
    }
}
