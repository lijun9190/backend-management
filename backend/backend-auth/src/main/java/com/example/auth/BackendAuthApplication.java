package com.example.auth;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 认证服务启动类。
 */
@MapperScan("com.example.auth.mapper")
@SpringBootApplication(scanBasePackages = "com.example")
public class BackendAuthApplication {

    public static void main(String[] args) {
        SpringApplication.run(BackendAuthApplication.class, args);
    }
}
