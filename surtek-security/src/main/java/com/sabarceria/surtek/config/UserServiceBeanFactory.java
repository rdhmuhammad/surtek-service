package com.sabarceria.surtek.config;

import com.sabarceria.surtek.repo.BaseUserSecurityService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Slf4j
public class UserServiceBeanFactory implements ApplicationContextAware, InitializingBean {

    @Autowired
    ListableBeanFactory listableBeanFactory;

    protected BaseUserSecurityService userSecurityRepo;

    private ApplicationContext applicationContext;

    public BaseUserSecurityService getUserRepo() {
        return userSecurityRepo;
    }

    @Override
    public void afterPropertiesSet() {
        initializeTypeBeanMap();
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    private void initializeTypeBeanMap() {
        Map<String, BaseUserSecurityService> beansWithAnnotation = applicationContext.getBeansOfType(BaseUserSecurityService.class);

        if (beansWithAnnotation.size() > 1) {
            throw new RuntimeException("diperbolehkan hanya satu user security repo");
        }

        if (beansWithAnnotation.isEmpty()) {
            throw new RuntimeException("user security wajid di definisikan");
        }

        for (String beanName : beansWithAnnotation.keySet()) {
            this.userSecurityRepo = listableBeanFactory.getBean(beanName, BaseUserSecurityService.class);
        }
        // Endpoint bypass scanning (@BypassFilter) now lives in FilterBypassRegistry.
    }
}
