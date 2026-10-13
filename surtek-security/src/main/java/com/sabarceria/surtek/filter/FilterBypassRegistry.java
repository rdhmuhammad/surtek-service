package com.sabarceria.surtek.filter;

import com.sabarceria.surtek.model.annotation.AllowAuth;
import com.sabarceria.surtek.model.annotation.AllowSignature;
import com.sabarceria.surtek.model.annotation.BypassFilter;
import com.sabarceria.surtek.model.annotation.EnableByPassSecurity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationContext;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.HttpMethod;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Holds, per filter type, the request matchers that are exempt from that filter.
 * <p>
 * Populated from {@link BypassFilter}, {@link AllowAuth}, and {@link AllowSignature} annotations
 * on controllers annotated with {@link EnableByPassSecurity}, as well as programmatic registration via
 * {@link #bypass(Class, String, HttpMethod)}.
 */
@Slf4j
@Component
public class FilterBypassRegistry implements SmartInitializingSingleton {

    private final ApplicationContext applicationContext;

    private final Map<Class<? extends BaseFilter>, List<RequestMatcher>> bypasses = new ConcurrentHashMap<>();

    public FilterBypassRegistry(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    /**
     * Programmatically exempt a path from a filter.
     *
     * @param filterType filter to skip (subclasses are skipped too)
     * @param antPattern Ant-style pattern, e.g. {@code /public/**} or {@code /users/{id}}
     * @param method     HTTP method, or {@code null} for any method
     */
    public void bypass(Class<? extends BaseFilter> filterType, String antPattern, HttpMethod method) {
        AntPathRequestMatcher matcher = method == null
                ? new AntPathRequestMatcher(antPattern)
                : new AntPathRequestMatcher(antPattern, method.name());
        bypasses.computeIfAbsent(filterType, k -> new CopyOnWriteArrayList<>()).add(matcher);
        log.info("Filter bypass registered: {} {} -> skip {}", method == null ? "*" : method, antPattern, filterType.getSimpleName());
    }

    /**
     * @return {@code true} if {@code filterType} must not run for this request.
     */
    public boolean isBypassed(Class<?> filterType, HttpServletRequest request) {
        HttpServletRequest normalizedRequest = (request.getServletPath() == null || request.getServletPath().isEmpty()) && request.getRequestURI() != null
                ? new HttpServletRequestWrapper(request) {
                    @Override
                    public String getServletPath() {
                        String uri = super.getRequestURI();
                        String context = super.getContextPath();
                        if (context != null && uri.startsWith(context)) {
                            return uri.substring(context.length());
                        }
                        return uri;
                    }
                }
                : request;

        for (Map.Entry<Class<? extends BaseFilter>, List<RequestMatcher>> entry : bypasses.entrySet()) {
            if (entry.getKey().isAssignableFrom(filterType)) {
                for (RequestMatcher matcher : entry.getValue()) {
                    if (matcher.matches(normalizedRequest)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public void afterSingletonsInstantiated() {
        applicationContext.getBeansWithAnnotation(EnableByPassSecurity.class)
                .values()
                .forEach(this::registerController);
    }

    private void registerController(Object controller) {
        Class<?> type = ClassUtils.getUserClass(controller);
        RequestMapping classMapping = AnnotatedElementUtils.findMergedAnnotation(type, RequestMapping.class);
        String[] basePaths = (classMapping == null || classMapping.path().length == 0)
                ? (classMapping != null && classMapping.value().length > 0 ? classMapping.value() : new String[]{""})
                : classMapping.path();

        Map<Method, Set<Class<? extends BaseFilter>>> annotatedMethods = MethodIntrospector.selectMethods(type,
                (MethodIntrospector.MetadataLookup<Set<Class<? extends BaseFilter>>>) this::resolveBypassedFilters);

        annotatedMethods.forEach((method, filterTypes) -> {
            if (filterTypes.isEmpty()) {
                return;
            }
            RequestMapping mapping = AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping.class);
            if (mapping == null) {
                log.warn("Security bypass annotation on {}#{} ignored: method has no request mapping", type.getSimpleName(), method.getName());
                return;
            }
            String[] subPaths = mapping.path().length > 0 ? mapping.path() : (mapping.value().length > 0 ? mapping.value() : new String[]{""});
            List<HttpMethod> methods = toHttpMethods(mapping.method());

            for (String base : basePaths) {
                for (String sub : subPaths) {
                    String pattern = joinPath(base, sub);
                    for (Class<? extends BaseFilter> filterType : filterTypes) {
                        if (methods.isEmpty()) {
                            bypass(filterType, pattern, null);
                        } else {
                            methods.forEach(m -> bypass(filterType, pattern, m));
                        }
                    }
                }
            }
        });
    }

    private Set<Class<? extends BaseFilter>> resolveBypassedFilters(Method method) {
        Set<Class<? extends BaseFilter>> result = new HashSet<>();

        BypassFilter bypass = AnnotatedElementUtils.findMergedAnnotation(method, BypassFilter.class);
        if (bypass != null) {
            Class<? extends BaseFilter>[] targets = bypass.value().length > 0 ? bypass.value() : bypass.filters();
            if (targets.length == 0) {
                result.add(BaseFilter.class);
            } else {
                result.addAll(Arrays.asList(targets));
            }
        }

        AllowAuth allowAuth = AnnotatedElementUtils.findMergedAnnotation(method, AllowAuth.class);
        if (allowAuth != null) {
            Class<? extends BaseFilter>[] targets = allowAuth.value().length > 0 ? allowAuth.value() : allowAuth.filters();
            if (targets.length == 0) {
                result.add(JwtAuthenticationFilter.class);
            } else {
                result.addAll(Arrays.asList(targets));
            }
        }

        AllowSignature allowSignature = AnnotatedElementUtils.findMergedAnnotation(method, AllowSignature.class);
        if (allowSignature != null) {
            Class<? extends BaseFilter>[] targets = allowSignature.value().length > 0 ? allowSignature.value() : allowSignature.filters();
            result.addAll(Arrays.asList(targets));
        }

        return result;
    }

    private static List<HttpMethod> toHttpMethods(RequestMethod[] requestMethods) {
        List<HttpMethod> result = new ArrayList<>(requestMethods.length);
        for (RequestMethod rm : requestMethods) {
            result.add(HttpMethod.valueOf(rm.name()));
        }
        return result;
    }

    static String joinPath(String base, String sub) {
        String joined = ("/" + base + "/" + sub).replaceAll("/+", "/");
        return joined.length() > 1 && joined.endsWith("/") ? joined.substring(0, joined.length() - 1) : joined;
    }
}
