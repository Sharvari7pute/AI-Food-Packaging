package com.packsmart.service.ai;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import com.google.genai.Client;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.HttpOptions;
import com.google.genai.types.Part;
import com.google.genai.types.ThinkingConfig;
import com.packsmart.config.GeminiProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Thin wrapper around the Google Gen AI SDK. Every call returns {@link Optional#empty()} when the key is missing,
 * the call fails or it takes longer than {@code gemini.timeout-seconds}, so callers always have a fallback path.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GeminiClient {

    private static final int MILLIS = 1000;
    private static final float TEMPERATURE = 0.2f;

    private final GeminiProperties props;
    private volatile Client client;

    public boolean isEnabled() {
        return props.isEnabled();
    }

    /** One-shot text generation. */
    public Optional<String> text(String systemPrompt, String userPrompt) {
        return generate(systemPrompt, List.of(userContent(userPrompt)), null);
    }

    /** One-shot generation constrained to JSON matching {@code jsonSchema}. */
    public Optional<String> json(String systemPrompt, String userPrompt, Map<String, Object> jsonSchema) {
        return generate(systemPrompt, List.of(userContent(userPrompt)), jsonSchema);
    }

    /** Multi-turn generation (chat). */
    public Optional<String> chat(String systemPrompt, List<Content> turns) {
        return generate(systemPrompt, turns, null);
    }

    public static Content userContent(String text) {
        return Content.builder().role("user").parts(Part.fromText(text)).build();
    }

    public static Content modelContent(String text) {
        return Content.builder().role("model").parts(Part.fromText(text)).build();
    }

    private Optional<String> generate(String systemPrompt, List<Content> contents, Map<String, Object> jsonSchema) {
        if (!isEnabled()) {
            return Optional.empty();
        }
        GenerateContentConfig.Builder cfg = GenerateContentConfig.builder()
                .systemInstruction(Content.fromParts(Part.fromText(systemPrompt)))
                .temperature(TEMPERATURE)
                .httpOptions(HttpOptions.builder().timeout(props.getTimeoutSeconds() * MILLIS).build());
        if (props.getModel().contains("flash")) {
            cfg.thinkingConfig(ThinkingConfig.builder().thinkingBudget(0).build());
        }
        if (jsonSchema != null) {
            cfg.responseMimeType("application/json").responseJsonSchema(jsonSchema);
        }
        GenerateContentConfig config = cfg.build();
        log.debug("Gemini prompt [system]: {}", systemPrompt);
        log.debug("Gemini prompt [contents]: {}", contents);
        try {
            GenerateContentResponse resp = CompletableFuture
                    .supplyAsync(() -> client().models.generateContent(props.getModel(), contents, config))
                    .get(props.getTimeoutSeconds(), TimeUnit.SECONDS);
            String text = resp.text();
            log.debug("Gemini response: {}", text);
            return text == null || text.isBlank() ? Optional.empty() : Optional.of(text.trim());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        } catch (Exception e) {
            log.warn("Gemini call failed, using fallback: {}", e.toString());
            return Optional.empty();
        }
    }

    private Client client() {
        Client c = client;
        if (c == null) {
            synchronized (this) {
                if (client == null) {
                    client = Client.builder().apiKey(props.getApiKey()).build();
                }
                c = client;
            }
        }
        return c;
    }
}
