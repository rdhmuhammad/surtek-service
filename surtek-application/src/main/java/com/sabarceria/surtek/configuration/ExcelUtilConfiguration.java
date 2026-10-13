package com.sabarceria.surtek.configuration;

import com.sabarceria.surtek.commons.ExcelUtil;
import com.sabarceria.surtek.commons.SheetColumn;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.Configuration;

import java.lang.reflect.Field;
import java.util.Set;

@Configuration
@Slf4j
public class ExcelUtilConfiguration {

    @Autowired
    private BasicPackageNameFinder basePackageNameFinder;


    @Bean
    public ExcelUtil ExcelUtil() throws ClassNotFoundException {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);

        scanner.addIncludeFilter((metadataReader, metadataReaderFactory) -> {
            try {
                String className = metadataReader.getClassMetadata().getClassName();
                Class<?> clazz = Class.forName(className);
                for (Field field : clazz.getDeclaredFields()) {
                    if (field.isAnnotationPresent(SheetColumn.class)) {
                        return true;
                    }
                }
                return false;
            } catch (ClassNotFoundException e) {
                return false;
                // Handle or log the exception
            }
        });

        ExcelUtil.UtilBuilder builder = ExcelUtil.builder();
        Set<BeanDefinition> candidateComponents = scanner.findCandidateComponents(basePackageNameFinder.basePackageName);
        for (BeanDefinition candidateComponent : candidateComponents) {
            builder.addClass(Class.forName(candidateComponent.getBeanClassName()));
        }

        return builder.build();
    }
}
