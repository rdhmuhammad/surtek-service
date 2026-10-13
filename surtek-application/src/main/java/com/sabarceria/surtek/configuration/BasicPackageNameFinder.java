package com.sabarceria.surtek.configuration;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Configuration;

import java.util.Objects;

@Configuration
public class BasicPackageNameFinder implements BeanFactoryPostProcessor, ApplicationContextAware {

    protected ApplicationContext applicationContext;

    protected String basePackageName;


    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        if (Objects.isNull(applicationContext)) {
            throw new RuntimeException("applicationContext is null");
        }
        getBasePackageName(beanFactory);
    }

    protected void getBasePackageName(ConfigurableListableBeanFactory beanFactory) {
        String[] beanNames = applicationContext.getBeanNamesForAnnotation(SpringBootApplication.class);
        for (String beanName : beanNames) {
            BeanDefinition beanDefinition = beanFactory.getBeanDefinition(beanName);
            String className = beanDefinition.getBeanClassName();
            try {
                Class<?> beanClass = Class.forName(className);
                this.basePackageName = beanClass.getPackage().getName();
                return;
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("No SpringBootApplication class found", e);
            }
        }
        throw new RuntimeException("No SpringBootApplication class found");
    }
}
