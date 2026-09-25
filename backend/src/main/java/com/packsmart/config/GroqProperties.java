package com.packsmart.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Groq (OpenAI-compatible API) - backup AI provider used only when every Gemini model fails. Key stays on the server. */
@Data
@ConfigurationProperties(prefix = "groq")
public class GroqProperties {
    private String apiKey;
    private String model = "openai/gpt-oss-120b";
    private String baseUrl = "https://api.groq.com/openai/v1";
    private int timeoutSeconds = 8;

    public boolean isEnabled() {
        return apiKey != null && !apiKey.isBlank();
    }
}
