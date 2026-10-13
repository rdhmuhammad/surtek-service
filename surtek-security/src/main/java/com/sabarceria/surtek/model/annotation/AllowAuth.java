package com.sabarceria.surtek.model.annotation;

import com.sabarceria.surtek.filter.BaseFilter;

import java.lang.annotation.*;

/**
 * Marks an endpoint as exempt from authentication or dynamically specified filters.
 * <p>
 * If {@code filters} or {@code value} is specified, those filters will be bypassed.
 * If left empty, default authentication filters (such as {@link com.sabarceria.surtek.filter.JwtAuthenticationFilter})
 * will be bypassed.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AllowAuth {

    /**
     * Filters that must NOT run for the annotated endpoint.
     */
    Class<? extends BaseFilter>[] value() default {};

    /**
     * Alias for {@link #value()}.
     */
    Class<? extends BaseFilter>[] filters() default {};
}
