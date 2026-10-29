package com.example.Smart_E_Commerce_System.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Global OpenAPI metadata shown at the top of Swagger UI.
 * Docs: /swagger-ui.html  |  Raw spec: /v3/api-docs
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI smartEcommerceOpenApi(@Value("${server.port:8080}") int port) {
        return new OpenAPI()
                .info(new Info()
                        .title("Smart E-Commerce API")
                        .version("v1")
                        .description("""
                                REST API for managing users, categories and products.
                                
                                All responses use a standard envelope: `{ "status", "message", "data" }`.
                                List endpoints support pagination (`page`, `size`) and sorting
                                (`sort=field,asc|desc`). GraphQL is available separately at `/graphql`.
                                """)
                        .contact(new Contact().name("Smart E-Commerce Team"))
                        .license(new License().name("For educational use")))
                .servers(List.of(
                        new Server().url("http://localhost:" + port).description("Local development")));
    }
}
