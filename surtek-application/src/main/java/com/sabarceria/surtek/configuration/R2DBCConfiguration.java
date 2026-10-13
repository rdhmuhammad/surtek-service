package com.sabarceria.surtek.configuration;

import com.sabarceria.surtek.base.BaseR2dbcRepositoryFactoryBean;
import io.r2dbc.postgresql.PostgresqlConnectionConfiguration;
import io.r2dbc.postgresql.PostgresqlConnectionFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.r2dbc.core.DefaultReactiveDataAccessStrategy;
import org.springframework.data.r2dbc.dialect.PostgresDialect;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;
import org.springframework.data.r2dbc.repository.support.R2dbcRepositoryFactory;
import org.springframework.r2dbc.core.DatabaseClient;

@Configuration
@EnableR2dbcRepositories(
        repositoryFactoryBeanClass = BaseR2dbcRepositoryFactoryBean.class)
@RequiredArgsConstructor
public class R2DBCConfiguration {


    @Value("${spring.r2dbc.host}")
    private String host;

    @Value("${spring.r2dbc.port}")
    private int port;

    @Value("${spring.r2dbc.database}")
    private String database;

    @Value("${spring.r2dbc.username}")
    private String username;

    @Value("${spring.r2dbc.password}")
    private String password;


    @Bean
    public R2dbcRepositoryFactory r2dbcRepositoryFactory() {
        return new R2dbcRepositoryFactory(r2dbc(), new DefaultReactiveDataAccessStrategy(new PostgresDialect()));
    }

    @Bean
    public DatabaseClient r2dbc() {
        return DatabaseClient.create(connectionFactory());
    }

    @Bean
    public PostgresqlConnectionFactory connectionFactory() {
        PostgresqlConnectionConfiguration config = PostgresqlConnectionConfiguration.builder()
                .host(host)
                .port(port)
                .database(database)
                .username(username)
                .password(password)
                .build();
        return new PostgresqlConnectionFactory(config);
    }
}
