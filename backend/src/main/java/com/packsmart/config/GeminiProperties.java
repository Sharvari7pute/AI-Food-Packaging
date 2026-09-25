package com.packsmart.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Gemini settings. The key never leaves the server. */
@Data
@ConfigurationProperties(prefix = "gemini")
public class GeminiProperties {
    private String apiKey;
    private String model = "gemini-3.8-flash";
    private int timeoutSeconds = 8;
    /** Thinking level for Gemini 3+ models (low keeps answers inside the timeout); blank = model default. */
    private String thinkingLevel = "low";
    /** Tried in order when the main model hits its quota or is unavailable (free-tier quotas are per model). */
    private java.util.List<String> fallbackModels = new java.util.ArrayList<>();

    /** Main model followed by the fallback models, without duplicates. */
    public java.util.List<String> modelChain() {
        java.util.LinkedHashSet<String> chain = new java.util.LinkedHashSet<>();
        chain.add(model);
        fallbackModels.stream().map(String::trim).filter(m -> !m.isEmpty()).forEach(chain::add);
        return java.util.List.copyOf(chain);
    }

    public boolean isEnabled() {
        return apiKey != null && !apiKey.isBlank();
    }
}
