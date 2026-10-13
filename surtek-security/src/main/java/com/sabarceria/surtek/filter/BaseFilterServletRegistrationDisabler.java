package com.sabarceria.surtek.filter;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.stereotype.Component;

/**
 * Spring Boot automatically registers every {@link javax.servlet.Filter} bean in the servlet
 * container. {@link BaseFilter}s must only run inside the Spring Security chain (where
 * {@link FilterRegistry} places them), so this registers a disabled
 * {@link FilterRegistrationBean} for each of them.
 */
@Component
public class BaseFilterServletRegistrationDisabler implements BeanDefinitionRegistryPostProcessor {

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        if (!(registry instanceof ListableBeanFactory beanFactory)) {
            return;
        }
        for (String filterBeanName : beanFactory.getBeanNamesForType(BaseFilter.class, true, false)) {
            String registrationName = filterBeanName + "ServletRegistration";
            if (registry.containsBeanDefinition(registrationName)) {
                continue;
            }
            registry.registerBeanDefinition(registrationName, BeanDefinitionBuilder
                    .genericBeanDefinition(FilterRegistrationBean.class)
                    .addPropertyReference("filter", filterBeanName)
                    .addPropertyValue("enabled", false)
                    .getBeanDefinition());
        }
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        // no-op
    }
}
