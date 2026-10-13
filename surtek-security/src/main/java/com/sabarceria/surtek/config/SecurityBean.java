package com.sabarceria.surtek.config;


import com.sabarceria.surtek.model.UserDetailsImpl;
import com.sabarceria.surtek.model.UserSecurityModel;
import com.sabarceria.surtek.repo.BaseUserSecurityService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.multipart.commons.CommonsMultipartResolver;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

@Slf4j
@Configuration
public class SecurityBean {

    @Bean
    public ListableBeanFactory listableBeanFactory() {
        return new DefaultListableBeanFactory();
    }

    @Autowired
    UserServiceBeanFactory userServiceBeanFactory;

    @Bean
    public BaseUserSecurityService userSecurityService() {
        return userServiceBeanFactory.getUserRepo();
    }

    @Autowired
    public BaseUserSecurityService userSecurityService;

    @Bean
    public UserDetailsService userDetailsService() {
        return username -> {
            log.info("find user by username {}", username);
            UserSecurityModel user = userSecurityService.findById(username);
            return UserDetailsImpl.build(user);
        };
    }

    @Bean
    public RequestMappingHandlerMapping mappingHandlerMapping() {
        return new RequestMappingHandlerMapping();
    }

    @Bean
    public CommonsMultipartResolver multipartResolver() {
        CommonsMultipartResolver resolver = new CommonsMultipartResolver();
        resolver.setDefaultEncoding("utf-8");
        return resolver;
    }

}
