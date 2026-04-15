package com.example.system;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 系统管理服务启动类。
 */
@MapperScan("com.example.system.mapper")
@SpringBootApplication(scanBasePackages = "com.example")
public class BackendSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(BackendSystemApplication.class, args);
    }
}
