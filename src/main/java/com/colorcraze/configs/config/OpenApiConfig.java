package com.colorcraze.configs.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

/**
 * Configuration class for OpenAPI/Swagger documentation.
 * Exposes a custom {@link OpenAPI} bean with API metadata including title, version, and description.
 */
@Configuration
public class OpenApiConfig {

    /**
     * Creates a custom OpenAPI bean for API documentation.
     *
     * @return a configured {@link OpenAPI} instance
     */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Color Craze API")
                        .version("1.0.0")
                        .description("REST API para gestión de usuarios y autenticación usando MongoDB."));
    }
}
