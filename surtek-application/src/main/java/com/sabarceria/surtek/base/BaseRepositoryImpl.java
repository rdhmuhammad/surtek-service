package com.sabarceria.surtek.base;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.data.support.PageableExecutionUtils;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.Predicate;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class BaseRepositoryImpl<T extends BaseEntity, ID extends Serializable> extends SimpleJpaRepository<T, ID> implements BaseRepository<T, ID> {
    private final EntityManager entityManager;

    private static final Logger logger = LoggerFactory.getLogger(BaseRepositoryImpl.class);

    public BaseRepositoryImpl(Class<T> domainClass, EntityManager entityManager) {
        super(domainClass, entityManager);
        this.entityManager = entityManager;
    }

    public BaseRepositoryImpl(JpaEntityInformation<T, ?> entityInformation, EntityManager entityManager) {
        super(entityInformation, entityManager);
        this.entityManager = entityManager;
    }

    public List<T> findAll(Specification<T> spec, int offset, int maxResults) {
        return this.findAll(spec, offset, maxResults, Sort.unsorted());
    }

    public List<T> findAllSpec(Specification<T> spec, PageRequest pageRequest) {
        return this.findAll(spec, Math.toIntExact(pageRequest.getOffset()), pageRequest.getPageSize(), pageRequest.getSort());
    }

    private Specification<T> filterSearch(String key, String... col) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicateList = new ArrayList<>();
            for (String c : col) {
                predicateList.add(criteriaBuilder.like(criteriaBuilder.lower(root.get(c)), "%" + key.toLowerCase() + "%"));
            }

            return !predicateList.isEmpty() ?
                    criteriaBuilder.or(predicateList.toArray(new Predicate[0])) :
                    null;
        };
    }

    public Page<T> findAllFilterPaged(String key, PageRequest pageRequest, String... col) {
        TypedQuery<T> query = this.getQuery(filterSearch(key, col), pageRequest);
        long count = this.count(filterSearch(key, col));

        return PageableExecutionUtils.getPage(query.getResultList(), pageRequest, () -> count);
    }

    @Override
    public void softDelete(T entity) {
        entity.setDeleted(Boolean.TRUE);
        entityManager.getTransaction().begin();
        entityManager.persist(entity);
        entityManager.getTransaction().commit();
    }

    public List<T> findAll(Specification<T> spec, int offset, int maxResults, Sort sort) {
        TypedQuery<T> query = this.getQuery(spec, sort);
        if (offset < 0) {
            logger.error("Offset must not be less than zero!");
            throw new IllegalArgumentException("Offset must not be less than zero!");
        } else if (maxResults < 1) {
            logger.error("Max results must not be less than one!");
            throw new IllegalArgumentException("Max results must not be less than one!");
        } else {
            query.setFirstResult(offset);
            query.setMaxResults(maxResults);
            return query.getResultList();
        }
    }
}
