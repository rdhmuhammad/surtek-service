package com.sabarceria.surtek.base;

import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.r2dbc.repository.support.R2dbcRepositoryFactory;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.RepositoryMetadata;

public class BaseR2dbcRepositoryFactoryBean extends R2dbcRepositoryFactory {

    private final R2dbcEntityTemplate template;

    public BaseR2dbcRepositoryFactoryBean(R2dbcEntityTemplate template) {
        super(template);
        this.template = template;
    }


    @Override
    protected Object getTargetRepository(
            RepositoryInformation information) {

        return new BaseR2dbcRepositoryImpl<>(template, information.getDomainType());
    }

    @Override
    protected Class<?> getRepositoryBaseClass(RepositoryMetadata metadata) {
        return BaseR2dbcRepositoryImpl.class;
    }
}
