package com.learnhub.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenAPIConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("LEARN-Hub API")
                .version("1.0.0")
                .description("API for LEARN-Hub activity recommendations and lesson planning"))
            .servers(List.of(
                new Server().url("http://localhost:5001").description("Development local server"),
                new Server().url("https://master-api.dev.amihalcea.com").description("Development homeserver"),
                new Server().url("https://learnhub-test.aet.cit.tum.de").description("Production server")
            ))
            .components(new Components()
                .addSecuritySchemes("BearerAuth", new SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
                    .description("JWT token for authentication. Include 'Bearer ' prefix in Authorization header.")))
            .addSecurityItem(new SecurityRequirement().addList("BearerAuth"));
    }
}
