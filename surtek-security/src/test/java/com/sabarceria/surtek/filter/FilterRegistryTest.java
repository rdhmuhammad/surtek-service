package com.sabarceria.surtek.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sabarceria.surtek.config.ModuleSpecificAuth;
import com.sabarceria.surtek.jwt.InMemoryRefreshTokenStore;
import com.sabarceria.surtek.jwt.JwtProperties;
import com.sabarceria.surtek.jwt.JwtTokenService;
import com.sabarceria.surtek.jwt.TokenPair;
import com.sabarceria.surtek.model.annotation.BypassFilter;
import com.sabarceria.surtek.model.annotation.EnableByPassSecurity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.core.annotation.Order;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.context.support.GenericApplicationContext;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FilterRegistryTest {

    private JwtTokenService jwtTokenService;
    private RefreshTokenFilter refreshTokenFilter;
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    private SimulationAuditFilter simulationAuditFilter;
    private FilterRegistry filterRegistry;
    private FilterBypassRegistry bypassRegistry;

    @Order(300)
    static class SimulationAuditFilter extends BaseFilter {
        private boolean executed = false;

        @Override
        protected void onFilterChain(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
                throws ServletException, IOException {
            this.executed = true;
            response.setHeader("X-Audit-Simulated", "true");
            chain.doFilter(request, response);
        }

        public boolean isExecuted() {
            return executed;
        }

        public void reset() {
            this.executed = false;
        }
    }

    @RestController
    @EnableByPassSecurity
    @RequestMapping("/api")
    static class TestController {

        @GetMapping("/public")
        @BypassFilter(JwtAuthenticationFilter.class)
        public String publicEndpoint() {
            return "public";
        }

        @GetMapping("/audit-exempt")
        @BypassFilter(SimulationAuditFilter.class)
        public String auditExemptEndpoint() {
            return "audit-exempt";
        }

        @GetMapping("/protected")
        public String protectedEndpoint() {
            return "protected";
        }
    }

    @BeforeEach
    void setUp() {
        JwtProperties props = new JwtProperties();
        props.setIssuer("test-issuer");
        props.setAccessTokenSecret(Base64.getEncoder().encodeToString("access-secret-key-32-bytes-long!".getBytes()));
        props.setRefreshTokenSecret(Base64.getEncoder().encodeToString("refresh-secret-key-32-bytes-long".getBytes()));
        props.setAccessTokenTtl(Duration.ofMinutes(15));
        props.setRefreshTokenTtl(Duration.ofDays(7));
        props.setClockSkew(Duration.ZERO);

        UserDetails testUser = new User("alice", "pwd", List.of(new SimpleGrantedAuthority("ROLE_USER")));
        UserDetailsService userDetailsService = username -> "alice".equals(username) ? testUser : null;

        jwtTokenService = new JwtTokenService(props, new InMemoryRefreshTokenStore(), userDetailsService, Clock.systemUTC());

        ObjectMapper mapper = new ObjectMapper();
        refreshTokenFilter = new RefreshTokenFilter(jwtTokenService, mapper, props);
        jwtAuthenticationFilter = new JwtAuthenticationFilter(jwtTokenService, userDetailsService, new StaticObjectProvider<>(null), false);
        simulationAuditFilter = new SimulationAuditFilter();

        // Register controller with Spring ApplicationContext for FilterBypassRegistry scanning
        GenericApplicationContext context = new GenericApplicationContext();
        context.registerBean("testController", TestController.class);
        context.refresh();

        bypassRegistry = new FilterBypassRegistry(context);
        bypassRegistry.afterSingletonsInstantiated();

        refreshTokenFilter.setBypassRegistry(bypassRegistry);
        jwtAuthenticationFilter.setBypassRegistry(bypassRegistry);
        simulationAuditFilter.setBypassRegistry(bypassRegistry);

        List<BaseFilter> filtersList = List.of(simulationAuditFilter, refreshTokenFilter, jwtAuthenticationFilter);
        filterRegistry = new FilterRegistry(new StaticObjectProvider<>(filtersList));
    }

    @Test
    void testFilterRegistryOrdersCorrectly() {
        List<BaseFilter> registered = filterRegistry.getFilters();
        assertEquals(3, registered.size());
        // Ordered by @Order: 100 (RefreshTokenFilter), 200 (JwtAuthenticationFilter), 300 (SimulationAuditFilter)
        assertInstanceOf(RefreshTokenFilter.class, registered.get(0));
        assertInstanceOf(JwtAuthenticationFilter.class, registered.get(1));
        assertInstanceOf(SimulationAuditFilter.class, registered.get(2));
    }

    @Test
    void testSimulationFilterExecutedInChain() throws Exception {
        UserDetails user = new User("alice", "pwd", List.of(new SimpleGrantedAuthority("ROLE_USER")));
        TokenPair pair = jwtTokenService.issue(user);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/protected");
        request.setServletPath("/api/protected");
        request.addHeader("Authorization", "Bearer " + pair.accessToken());
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        simulationAuditFilter.reset();

        // Run through simulation filter
        simulationAuditFilter.doFilter(request, response, chain);

        assertTrue(simulationAuditFilter.isExecuted());
        assertEquals("true", response.getHeader("X-Audit-Simulated"));
    }

    @Test
    void testBypassFilterSkipsSimulationFilter() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/audit-exempt");
        request.setServletPath("/api/audit-exempt");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        simulationAuditFilter.reset();
        simulationAuditFilter.doFilter(request, response, chain);

        // SimulationAuditFilter should be bypassed
        assertFalse(simulationAuditFilter.isExecuted());
        assertNull(response.getHeader("X-Audit-Simulated"));
    }

    @Test
    void testBypassFilterBypassesJwtAuthenticationFilter() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/public");
        request.setServletPath("/api/public");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        // Request has no Bearer token, but @BypassFilter should bypass JwtAuthenticationFilter
        jwtAuthenticationFilter.doFilter(request, response, chain);

        // Status remains 200 (not rejected)
        assertEquals(200, response.getStatus());
    }

    private static class StaticObjectProvider<T> implements ObjectProvider<T> {
        private final List<T> list;

        @SuppressWarnings("unchecked")
        StaticObjectProvider(Object singleOrList) {
            if (singleOrList == null) {
                this.list = List.of();
            } else if (singleOrList instanceof List) {
                this.list = (List<T>) singleOrList;
            } else {
                this.list = List.of((T) singleOrList);
            }
        }

        @Override public T getObject(Object... args) { return list.isEmpty() ? null : list.get(0); }
        @Override public T getIfAvailable() { return list.isEmpty() ? null : list.get(0); }
        @Override public T getIfUnique() { return list.size() == 1 ? list.get(0) : null; }
        @Override public T getObject() { return list.get(0); }
        @Override public java.util.stream.Stream<T> orderedStream() {
            // Sort by @Order like Spring does
            return list.stream().sorted((a, b) -> {
                Order oA = a.getClass().getAnnotation(Order.class);
                Order oB = b.getClass().getAnnotation(Order.class);
                int vA = oA != null ? oA.value() : Integer.MAX_VALUE;
                int vB = oB != null ? oB.value() : Integer.MAX_VALUE;
                return Integer.compare(vA, vB);
            });
        }
    }
}
