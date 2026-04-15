package com.example.auth;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ApplicationYamlConfigTest {

    @Test
    void shouldParseExpectedDatasourceAndRedisPasswords() {
        YamlPropertiesFactoryBean factoryBean = new YamlPropertiesFactoryBean();
        factoryBean.setResources(new ClassPathResource("application.yml"));

        Properties properties = factoryBean.getObject();

        assertEquals("123456", properties.getProperty("spring.datasource.password"));
        assertEquals("imooc", properties.getProperty("spring.redis.password"));
    }
}
