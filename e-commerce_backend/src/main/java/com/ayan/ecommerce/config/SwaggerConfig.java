package com.ayan.ecommerce.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {
    @Bean
    public OpenAPI customOpenAPI(){
        return new OpenAPI()
                .info(
                        new Info()
                                .title("E-Commerce Backend API")
                                .version("1.0")
                                .description(
                                        "Spring Boot Backend Engineering Project"
                                )
                )

                .addSecurityItem(
                        new SecurityRequirement()
                                .addList("bearerAuth")
                )

                .schemaRequirement(
                        "bearerAuth",

                        new SecurityScheme()
                                .name("bearerAuth")
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                );
    }
}
