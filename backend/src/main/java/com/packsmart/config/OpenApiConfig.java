package com.packsmart.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI packSmartOpenApi() {
        return new OpenAPI().info(new Info()
                .title("PackSmart API")
                .version("0.1.0")
                .description("Food packaging recommendation engine (SIH26236). "
                        + "Every packaging decision comes from the deterministic engine; Gemini only explains."));
    }
}
