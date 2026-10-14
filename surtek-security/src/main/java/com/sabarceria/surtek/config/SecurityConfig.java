package com.sabarceria.surtek.config;

import com.sabarceria.surtek.filter.BaseFilter;
import com.sabarceria.surtek.filter.FilterRegistry;
import com.sabarceria.surtek.filter.SecurityAuthenticationEntryPoint;
import com.sabarceria.surtek.repo.BaseUserSecurityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;

import com.sabarceria.surtek.oauth2.HttpCookieOAuth2AuthorizationRequestRepository;
import com.sabarceria.surtek.oauth2.OAuth2AuthenticationFailureHandler;
import com.sabarceria.surtek.oauth2.OAuth2AuthenticationSuccessHandler;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * See <a href="https://www.baeldung.com/spring-deprecated-websecurityconfigureradapter">baeldung</a> <br></br>
 * See <a href="https://spring.io/blog/2022/02/21/spring-security-without-the-websecurityconfigureradapter">baeldung</a> <br></br>
 * See <a href="https://medium.com/code-with-farhan/spring-security-2023-4110f1e33b47">medium.com</a> <br></br>
 * See <a href="https://github.com/chhinhua/spring-security-tutorial-2023">medium.com</a>
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@EnableWebSecurity
@EnableGlobalMethodSecurity(jsr250Enabled = true)
public class SecurityConfig {

    private final SecurityAuthenticationEntryPoint securityAuthenticationEntryPoint;

    private final UserDetailsService userDetailsService;

    private final BaseUserSecurityService userSecurityService;

    private final FilterRegistry filterRegistry;

    private final ObjectProvider<ClientRegistrationRepository> clientRegistrationRepositoryProvider;

    private final ObjectProvider<OAuth2AuthenticationSuccessHandler> oauth2SuccessHandlerProvider;

    private final ObjectProvider<OAuth2AuthenticationFailureHandler> oauth2FailureHandlerProvider;

    private final ObjectProvider<HttpCookieOAuth2AuthorizationRequestRepository> cookieAuthorizationRequestRepositoryProvider;

    @Bean
    public CorsConfiguration corsConfiguration() {
        CorsConfiguration cors = userSecurityService.applyCors();
        if (Objects.nonNull(cors))
            return cors;

        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "PATCH", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        return configuration;
    }

    /**
     * One chain containing every {@link BaseFilter} from the {@link FilterRegistry}, in registry order.
     * <p>
     * Access is enforced by those filters (e.g. {@code JwtAuthenticationFilter} rejects any request
     * without a valid token unless the endpoint has {@code @BypassFilter}) and by {@code @RolesAllowed}.
     * The URL authorization layer therefore permits all requests: if no filter enforces
     * authentication, endpoints are public.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors().configurationSource(request -> corsConfiguration())
                .and()
                // Stateless bearer-token API: no session cookie, so CSRF does not apply.
                .csrf().disable()
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .exceptionHandling().authenticationEntryPoint(securityAuthenticationEntryPoint)
                .and()
                .authenticationProvider(authenticationProvider())
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());

        // Filters added before the same anchor keep their insertion order (stable sort),
        // so the chain order is exactly the registry order.
        for (BaseFilter filter : filterRegistry.getFilters()) {
            http.addFilterBefore(filter, UsernamePasswordAuthenticationFilter.class);
        }

        ClientRegistrationRepository clientRegistrationRepository = clientRegistrationRepositoryProvider.getIfAvailable();
        if (clientRegistrationRepository != null) {
            http.oauth2Login(oauth2 -> {
                oauth2.clientRegistrationRepository(clientRegistrationRepository);
                cookieAuthorizationRequestRepositoryProvider.ifAvailable(repo ->
                        oauth2.authorizationEndpoint().authorizationRequestRepository(repo)
                );
                oauth2SuccessHandlerProvider.ifAvailable(oauth2::successHandler);
                oauth2FailureHandlerProvider.ifAvailable(oauth2::failureHandler);
            });
        }

        return http.build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        final DaoAuthenticationProvider authenticationProvider = new DaoAuthenticationProvider();
        authenticationProvider.setUserDetailsService(userDetailsService);
        authenticationProvider.setPasswordEncoder(passwordEncoder());
        return authenticationProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return NoOpPasswordEncoder.getInstance();
    }
}
