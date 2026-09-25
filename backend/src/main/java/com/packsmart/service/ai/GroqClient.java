package com.packsmart.service.ai;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.packsmart.config.GroqProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Backup LLM via Groq's OpenAI-compatible chat completions API. Called by {@link GeminiClient} only after every Gemini
 * model failed (quota, outage, timeout). Returns {@link Optional#empty()} on any problem so callers use the template.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GroqClient {

    private static final float TEMPERATURE = 0.2f;
    private static final int HTTP_OK_MIN = 200;
    private static final int HTTP_OK_MAX = 299;

    private final GroqProperties props;
    private final ObjectMapper json;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(4)).build();

    /** One chat turn: role "user" or "assistant". */
    public record Message(String role, String content) {
    }

    public boolean isEnabled() {
        return props.isEnabled();
    }

    /**
     * Sends the system prompt + messages. {@code jsonMode} asks for a single JSON object (the prompts already say
     * "Return JSON", which Groq's JSON mode requires).
     */
    public Optional<String> chat(String systemPrompt, List<Message> messages, boolean jsonMode) {
        if (!isEnabled()) {
            return Optional.empty();
        }
        try {
            List<Map<String, String>> msgs = new ArrayList<>();
            msgs.add(Map.of("role", "system", "content", systemPrompt));
            messages.forEach(m -> msgs.add(Map.of("role", "assistant".equals(m.role()) ? "assistant" : "user", "content", m.content())));
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", props.getModel());
            body.put("messages", msgs);
            body.put("temperature", TEMPERATURE);
            if (jsonMode) {
                body.put("response_format", Map.of("type", "json_object"));
            }
            HttpRequest req = HttpRequest.newBuilder(URI.create(props.getBaseUrl().replaceAll("/$", "") + "/chat/completions"))
                    .timeout(Duration.ofSeconds(props.getTimeoutSeconds()))
                    .header("Authorization", "Bearer " + props.getApiKey())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body)))
                    .build();
            log.debug("Groq prompt [system]: {}", systemPrompt);
            log.debug("Groq prompt [messages]: {}", messages);
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() < HTTP_OK_MIN || resp.statusCode() > HTTP_OK_MAX) {
                log.warn("Groq {} failed: HTTP {} {}", props.getModel(), resp.statusCode(),
                        resp.body().substring(0, Math.min(160, resp.body().length())));
                return Optional.empty();
            }
            JsonNode content = json.readTree(resp.body()).path("choices").path(0).path("message").path("content");
            String text = content.isTextual() ? content.asText().trim() : "";
            log.debug("Groq response: {}", text);
            return text.isEmpty() ? Optional.empty() : Optional.of(text);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        } catch (Exception e) {
            log.warn("Groq call failed, using template fallback: {}", e.toString());
            return Optional.empty();
        }
    }
}
