package com.packsmart.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** General app settings ({@code app.*}). */
@Data
@ConfigurationProperties(prefix = "app")
public class AppProperties {
    private String frontendUrl = "http://localhost:3000";
    private boolean reseedOnStart = false;
    private String disclaimer = "Prototype estimates based on literature values. Validate with lab shelf-life testing.";

    /** Frontend URL without a trailing slash. */
    public String frontendBase() {
        return frontendUrl.endsWith("/") ? frontendUrl.substring(0, frontendUrl.length() - 1) : frontendUrl;
    }
}
