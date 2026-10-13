package com.sabarceria.surtek.base;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.NoRepositoryBean;

import java.io.Serializable;
import java.util.List;

@NoRepositoryBean
public interface BaseRepository<T extends BaseEntity, ID extends Serializable> extends JpaRepository<T, ID>, JpaSpecificationExecutor<T> {
    List<T> findAll(Specification<T> spec, int offset, int total, Sort sortBy);

    List<T> findAllSpec(Specification<T> var1, PageRequest var2);

    Page<T> findAllFilterPaged(String key, PageRequest pageRequest, String... col);

    void softDelete(T entity);

}
