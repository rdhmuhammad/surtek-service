package com.sabarceria.surtek.repo;

import com.sabarceria.surtek.config.ModuleSpecificAuth;
import com.sabarceria.surtek.model.UserDetailsImpl;
import com.sabarceria.surtek.model.UserSecurityModel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.MultiValueMap;
import org.springframework.web.cors.CorsConfiguration;

import java.util.ArrayList;
import java.util.List;

public abstract class BaseUserSecurityService {
    public abstract UserSecurityModel findById(String id);

    public abstract void authenticateUser(String username);

    public abstract void authenticateUser(String username, Boolean status);

    public void setSecurityContext(UserSecurityModel userSecurityModel, String token) {
        UserDetailsImpl userDetailAuth = UserDetailsImpl.build(userSecurityModel);
        setSecurityContext(userDetailAuth, token);
    }

    public void setSecurityContext(UserDetailsImpl userSecurityModel, String token) {
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(userSecurityModel, token, userSecurityModel.getAuthorities());
        authenticationToken.setDetails(userSecurityModel);
        SecurityContext emptyContext = SecurityContextHolder.createEmptyContext();
        emptyContext.setAuthentication(authenticationToken);
        SecurityContextHolder.setContext(emptyContext);
    }

    public abstract CorsConfiguration applyCors();

    // ================= Whitelist Module any endpoint =================
    @Bean()
    @ConditionalOnMissingBean(ModuleSpecificAuth.class)
    public ModuleSpecificAuth moduleSpecificAuth() {
        return ModuleSpecificAuth.builder().build();
    }

    // ================= Whitelist any endpoint =================
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PathAllowed {
        private String path;

        private HttpMethod httpMethod;
    }

    private final List<PathAllowed> allowedSignature = new ArrayList<>();

    private final List<PathAllowed> allowedAuth = new ArrayList<>();

    public void addAllowedSignature(String path, HttpMethod method) {
        this.allowedSignature.add(new PathAllowed(path, method));
    }

    @SuppressWarnings({})
    public List<PathAllowed> getAllowedSignature() {
        return allowedSignature;
    }

    public void addAllowedAuth(String path, HttpMethod method) {
        this.allowedAuth.add(new PathAllowed(path, method));
    }

    @SuppressWarnings({})
    public List<PathAllowed> getAllowedAuth() {
        return allowedAuth;
    }

}
