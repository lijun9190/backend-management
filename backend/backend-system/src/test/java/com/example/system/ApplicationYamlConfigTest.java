package com.example.system;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ApplicationYamlConfigTest {

    @Test
    void shouldUseEnvironmentPlaceholdersForSensitiveConnectionConfig() {
        YamlPropertiesFactoryBean factoryBean = new YamlPropertiesFactoryBean();
        factoryBean.setResources(new ClassPathResource("application.yml"));

        Properties properties = factoryBean.getObject();

        assertTrue(properties.getProperty("spring.datasource.password").contains("MYSQL_PASSWORD"));
        assertTrue(properties.getProperty("spring.redis.password").contains("REDIS_PASSWORD"));
    }
}
