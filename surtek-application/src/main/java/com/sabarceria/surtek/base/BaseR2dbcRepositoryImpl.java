package com.sabarceria.surtek.base;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.relational.core.query.Query;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class BaseR2dbcRepositoryImpl<T, ID> implements BaseR2dbcRepository<T, ID> {

    private final R2dbcEntityTemplate template;

    private final Class<T> entityClass;

    public BaseR2dbcRepositoryImpl(R2dbcEntityTemplate template, Class<T> entityClass) {
        this.template = template;
        this.entityClass = entityClass;
    }

    @Override
    public Mono<Boolean> exists(Query query) {
        return template.
                exists(query, entityClass);
    }

    @Override
    public Mono<Page<T>> findAll(Query query, Pageable pageable) {
        Query paginatedQuery = query.limit(pageable.getPageSize())
                .offset(pageable.getOffset());

        Mono<Long> count = template.count(query, entityClass);
        Flux<T> all = template.select(entityClass).matching(paginatedQuery).all();

        return all.collectList()
                .zipWith(count)
                .map(data-> new PageImpl<>(data.getT1(), pageable, data.getT2()));
    }
}
