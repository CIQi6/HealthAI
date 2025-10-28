package com.example.healthai.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI healthAiOpenAPI() {
        return new OpenAPI()
                .components(new Components())
                .info(new Info()
                        .title("HealthAi API")
                        .version("0.0.1")
                        .description("HealthAi 后端接口文档"));
    }
}
