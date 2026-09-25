package com.packsmart.config;

import java.util.LinkedHashSet;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Allows the local dev frontend and the deployed frontend ({@code FRONTEND_URL}). */
@Configuration
@RequiredArgsConstructor
public class CorsConfig implements WebMvcConfigurer {

    private final AppProperties app;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        Set<String> origins = new LinkedHashSet<>();
        origins.add("http://localhost:3000");
        for (String url : app.getFrontendUrl().split(",")) {
            String trimmed = url.trim();
            if (!trimmed.isEmpty()) {
                origins.add(trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed);
            }
        }
        registry.addMapping("/api/**")
                .allowedOrigins(origins.toArray(String[]::new))
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .maxAge(3600);
    }
}
