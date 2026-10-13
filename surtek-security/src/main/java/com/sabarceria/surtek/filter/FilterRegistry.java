package com.sabarceria.surtek.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Collects every {@link BaseFilter} bean in the application context, in chain order.
 * <p>
 * Ordering follows {@code @Order} / {@link org.springframework.core.Ordered} on the filter
 * (lower value runs first; unordered filters run last). To add a filter, extend
 * {@link BaseFilter} and declare it as a bean — nothing else needs to change.
 */
@Slf4j
@Component
public class FilterRegistry {

    private final List<BaseFilter> filters;

    public FilterRegistry(ObjectProvider<BaseFilter> filters) {
        // orderedStream() respects @Order/Ordered and works through proxies; empty if none defined.
        this.filters = filters.orderedStream().collect(Collectors.toUnmodifiableList());
        log.info("Security filter chain: {}", this.filters.stream()
                .map(f -> f.getClass().getSimpleName())
                .collect(Collectors.joining(" -> ", "[", "]")));
    }

    public List<BaseFilter> getFilters() {
        return filters;
    }
}
