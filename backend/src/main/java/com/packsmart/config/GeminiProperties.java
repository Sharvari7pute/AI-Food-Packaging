package com.packsmart.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Gemini settings. The key never leaves the server. */
@Data
@ConfigurationProperties(prefix = "gemini")
public class GeminiProperties {
    private String apiKey;
    private String model = "gemini-2.5-flash";
    private int timeoutSeconds = 8;

    public boolean isEnabled() {
        return apiKey != null && !apiKey.isBlank();
    }
}
