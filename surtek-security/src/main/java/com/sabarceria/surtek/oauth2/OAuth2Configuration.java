package com.sabarceria.surtek.oauth2;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sabarceria.surtek.jwt.JwtTokenService;
import com.sabarceria.surtek.repo.BaseUserSecurityService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientProperties;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientPropertiesRegistrationAdapter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Configuration
@ConditionalOnClass(ClientRegistrationRepository.class)
@EnableConfigurationProperties({OAuth2Properties.class, OAuth2ClientProperties.class})
public class OAuth2Configuration {

    @Bean
    @ConditionalOnMissingBean(OAuth2UserProvisioner.class)
    public OAuth2UserProvisioner oAuth2UserProvisioner(UserDetailsService userDetailsService,
                                                       ObjectProvider<BaseUserSecurityService> userSecurityService) {
        return new DefaultOAuth2UserProvisioner(userDetailsService, userSecurityService);
    }

    @Bean
    @ConditionalOnMissingBean(OAuth2AuthenticationSuccessHandler.class)
    public OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler(
            JwtTokenService jwtTokenService,
            OAuth2UserProvisioner userProvisioner,
            OAuth2Properties properties,
            HttpCookieOAuth2AuthorizationRequestRepository cookieRepository,
            ObjectMapper objectMapper) {
        return new OAuth2AuthenticationSuccessHandler(
                jwtTokenService,
                userProvisioner,
                properties,
                cookieRepository,
                objectMapper
        );
    }

    @Bean
    @ConditionalOnMissingBean(OAuth2AuthenticationFailureHandler.class)
    public OAuth2AuthenticationFailureHandler oAuth2AuthenticationFailureHandler(
            OAuth2Properties properties,
            HttpCookieOAuth2AuthorizationRequestRepository cookieRepository) {
        return new OAuth2AuthenticationFailureHandler(properties, cookieRepository);
    }

    @Bean
    @ConditionalOnMissingBean(ClientRegistrationRepository.class)
    public InMemoryClientRegistrationRepository clientRegistrationRepository(OAuth2ClientProperties properties) {
        if (properties == null || properties.getRegistration() == null || properties.getRegistration().isEmpty()) {
            return null;
        }

        // Auto-configure Discord OAuth2 provider defaults if declared under registration
        if (properties.getRegistration().containsKey("discord") && !properties.getProvider().containsKey("discord")) {
            OAuth2ClientProperties.Provider discord = new OAuth2ClientProperties.Provider();
            discord.setAuthorizationUri("https://discord.com/api/oauth2/authorize");
            discord.setTokenUri("https://discord.com/api/oauth2/token");
            discord.setUserInfoUri("https://discord.com/api/users/@me");
            discord.setUserNameAttribute("id");
            properties.getProvider().put("discord", discord);
            log.info("Auto-configured Discord OAuth2 provider endpoints");
        }

        Map<String, ClientRegistration> registrations = OAuth2ClientPropertiesRegistrationAdapter
                .getClientRegistrations(properties);
        if (registrations == null || registrations.isEmpty()) {
            return null;
        }
        return new InMemoryClientRegistrationRepository(new ArrayList<>(registrations.values()));
    }
}
