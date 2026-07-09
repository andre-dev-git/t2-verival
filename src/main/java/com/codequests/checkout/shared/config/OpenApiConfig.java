package com.codequests.checkout.shared.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Cart Checkout + Mock Payment System API")
                        .version("1.0")
                        .description("A single-service backend for cart management, checkout, and mock payment processing"));
    }
}

