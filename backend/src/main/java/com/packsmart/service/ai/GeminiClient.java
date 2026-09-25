package com.packsmart.service.ai;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.google.genai.Client;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.HttpOptions;
import com.google.genai.types.HttpRetryOptions;
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

    private static final float TEMPERATURE = 0.2f;

    private final GeminiProperties props;
    private final GroqClient groq;
    private volatile Client client;

    /** True when any AI provider is configured (Gemini primary, Groq backup). */
    public boolean isEnabled() {
        return props.isEnabled() || groq.isEnabled();
    }

    /** One-shot text generation. */
    public Optional<String> text(String systemPrompt, String userPrompt) {
        return generate(systemPrompt, List.of(userContent(userPrompt)), null);
    }

    /** One-shot generation constrained to JSON matching {@code jsonSchema} (empty map = JSON without a schema). */
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

    /** Gemini model chain first; if none answers, Groq; if that fails too, empty (callers use templates). */
    private Optional<String> generate(String systemPrompt, List<Content> contents, Map<String, Object> jsonSchema) {
        Optional<String> answer = props.isEnabled() ? generateGemini(systemPrompt, contents, jsonSchema) : Optional.empty();
        if (answer.isPresent() || !groq.isEnabled()) {
            return answer;
        }
        log.info("Gemini unavailable - trying Groq backup");
        return groq.chat(systemPrompt, toMessages(contents), jsonSchema != null);
    }

    private static List<GroqClient.Message> toMessages(List<Content> contents) {
        return contents.stream()
                .map(c -> new GroqClient.Message(c.role().orElse("user").equals("model") ? "assistant" : "user",
                        String.join("\n", c.parts().orElse(List.of()).stream().map(p -> p.text().orElse("")).toList())))
                .toList();
    }

    private Optional<String> generateGemini(String systemPrompt, List<Content> contents, Map<String, Object> jsonSchema) {
        log.debug("Gemini prompt [system]: {}", systemPrompt);
        log.debug("Gemini prompt [contents]: {}", contents);
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(props.getTimeoutSeconds());
        for (String model : props.modelChain()) {
            long left = deadline - System.nanoTime();
            if (left <= 0) {
                log.warn("Gemini time budget used up");
                return Optional.empty();
            }
            GenerateContentConfig config = config(model, systemPrompt, jsonSchema, left);
            try {
                GenerateContentResponse resp = CompletableFuture
                        .supplyAsync(() -> client().models.generateContent(model, contents, config))
                        .get(left, TimeUnit.NANOSECONDS);
                String text = resp.text();
                log.debug("Gemini response ({}): {}", model, text);
                return text == null || text.isBlank() ? Optional.empty() : Optional.of(text.trim());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return Optional.empty();
            } catch (TimeoutException e) {
                log.warn("Gemini {} timed out", model);
                return Optional.empty();
            } catch (Exception e) {
                // Quota (429), retired model (404), overload (503) or unsupported option (400): try the next model.
                log.warn("Gemini {} failed: {}", model, firstLine(e));
            }
        }
        log.warn("All Gemini models failed");
        return Optional.empty();
    }

    private GenerateContentConfig config(String model, String systemPrompt, Map<String, Object> jsonSchema, long nanosLeft) {
        int timeoutMs = (int) Math.max(1, TimeUnit.NANOSECONDS.toMillis(nanosLeft));
        GenerateContentConfig.Builder cfg = GenerateContentConfig.builder()
                .systemInstruction(Content.fromParts(Part.fromText(systemPrompt)))
                .temperature(TEMPERATURE)
                // One attempt only: on a quota (429) or overload (503) error we move on at once instead of retrying.
                .httpOptions(HttpOptions.builder().timeout(timeoutMs)
                        .retryOptions(HttpRetryOptions.builder().attempts(1).build()).build());
        // Keep answers fast: no thinking on 2.5 Flash, low thinking level on Gemini 3+ models.
        if (model.startsWith("gemini-2.5-flash")) {
            cfg.thinkingConfig(ThinkingConfig.builder().thinkingBudget(0).build());
        } else if (props.getThinkingLevel() != null && !props.getThinkingLevel().isBlank()) {
            cfg.thinkingConfig(ThinkingConfig.builder().thinkingLevel(props.getThinkingLevel()).build());
        }
        if (jsonSchema != null) {
            cfg.responseMimeType("application/json");
            if (!jsonSchema.isEmpty()) {
                cfg.responseJsonSchema(jsonSchema);
            }
        }
        return cfg.build();
    }

    private static String firstLine(Exception e) {
        Throwable t = e.getCause() != null ? e.getCause() : e;
        String msg = String.valueOf(t.getMessage());
        int cut = msg.indexOf('.', msg.indexOf(' ') + 1);
        return t.getClass().getSimpleName() + ": " + (cut > 0 && cut < 160 ? msg.substring(0, cut) : msg.substring(0, Math.min(160, msg.length())));
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
