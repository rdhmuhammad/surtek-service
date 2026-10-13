package com.sabarceria.surtek.base;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.relational.core.query.Query;
import org.springframework.data.repository.NoRepositoryBean;
import reactor.core.publisher.Mono;

@NoRepositoryBean
public interface BaseR2dbcRepository<T, ID>{

    Mono<Boolean> exists(Query query);

    Mono<Page<T>> findAll(Query query, Pageable pageable);
}
