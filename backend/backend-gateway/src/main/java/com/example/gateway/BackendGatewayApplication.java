package com.example.gateway;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import com.example.gateway.config.IgnoreWhiteProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * 网关启动类。
 *
 * 网关负责统一入口、跨域处理、白名单放行和 token 基础校验。
 */
@SpringBootApplication(
    scanBasePackages = "com.example",
    exclude = {DataSourceAutoConfiguration.class}
)
@EnableConfigurationProperties(IgnoreWhiteProperties.class)
public class BackendGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(BackendGatewayApplication.class, args);
    }
}
