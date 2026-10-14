package com.sabarceria.surtek.model.annotation;

import com.sabarceria.surtek.filter.BaseFilter;

import java.lang.annotation.*;

/**
 * Marks a controller endpoint as exempt from one or more {@link BaseFilter} implementations.
 * <p>
 * Because the value is a filter <b>type</b>, it automatically follows whatever filters are registered in the
 * {@link com.sabarceria.surtek.filter.FilterRegistry}. Adding a new filter needs no new annotation.
 * <p>
 * The declaring controller must be annotated with {@link EnableByPassSecurity}.
 *
 * <pre>{@code
 * @RestController
 * @EnableByPassSecurity
 * @RequestMapping("/auth")
 * class AuthController {
 *
 *     @PostMapping("/login")
 *     @BypassFilter(JwtAuthenticationFilter.class)   // public: no access token needed
 *     TokenPair login(@RequestBody LoginRequest req) { ... }
 * }
 * }</pre>
 *
 * A bypass on a filter type also applies to its subclasses.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface BypassFilter {

    /**
     * Filters that must NOT run for the annotated endpoint.
     */
    Class<? extends BaseFilter>[] value() default {};

    /**
     * Alias for {@link #value()}.
     */
    Class<? extends BaseFilter>[] filters() default {};
}
