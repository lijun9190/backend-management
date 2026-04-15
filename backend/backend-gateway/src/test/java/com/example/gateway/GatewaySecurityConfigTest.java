package com.example.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = {
                BackendGatewayApplication.class,
                GatewaySecurityConfigTest.TestApiConfiguration.class
        }
)
@AutoConfigureWebTestClient
@TestPropertySource(properties = {
        "security.ignore.urls[0]=/test/ping",
        "spring.cloud.gateway.routes[0].id=test-route",
        "spring.cloud.gateway.routes[0].uri=http://127.0.0.1:65535",
        "spring.cloud.gateway.routes[0].predicates[0]=Path=/unused/**"
})
class GatewaySecurityConfigTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void shouldAllowAnonymousPostWithoutCsrfProtection() {
        webTestClient.post()
                .uri("/test/ping")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"ping\":true}")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).isEqualTo("pong");
    }

    @Configuration
    static class TestApiConfiguration {

        @Bean
        TestController testController() {
            return new TestController();
        }
    }

    @RestController
    static class TestController {

        @PostMapping("/test/ping")
        Mono<String> ping() {
            return Mono.just("pong");
        }
    }
}
