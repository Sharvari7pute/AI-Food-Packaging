package com.packsmart.ai;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.packsmart.config.GeminiProperties;
import com.packsmart.config.GroqProperties;
import com.packsmart.service.ai.GeminiClient;
import com.packsmart.service.ai.GroqClient;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Groq is called (OpenAI-compatible format) when Gemini is unavailable; any Groq error falls through to templates. */
class GroqFallbackTest {

    private final ObjectMapper json = new ObjectMapper();
    private final AtomicReference<JsonNode> lastRequest = new AtomicReference<>();
    private final AtomicReference<String> lastAuth = new AtomicReference<>();
    private final AtomicInteger status = new AtomicInteger(200);
    private HttpServer server;
    private GeminiClient ai;

    @BeforeEach
    void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/openai/v1/chat/completions", ex -> {
            lastAuth.set(ex.getRequestHeaders().getFirst("Authorization"));
            lastRequest.set(json.readTree(ex.getRequestBody()));
            byte[] body = (status.get() == 200
                    ? "{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":\" Hello from Groq \"}}]}"
                    : "{\"error\":{\"message\":\"rate limited\"}}").getBytes(StandardCharsets.UTF_8);
            ex.getResponseHeaders().add("Content-Type", "application/json");
            ex.sendResponseHeaders(status.get(), body.length);
            try (OutputStream os = ex.getResponseBody()) {
                os.write(body);
            }
        });
        server.start();

        GeminiProperties gemini = new GeminiProperties();
        gemini.setApiKey("");
        GroqProperties groq = new GroqProperties();
        groq.setApiKey("test-key");
        groq.setModel("test-model");
        groq.setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort() + "/openai/v1");
        ai = new GeminiClient(gemini, new GroqClient(groq, json));
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    @Test
    void groqAnswersWhenGeminiIsUnavailable() {
        assertThat(ai.isEnabled()).isTrue();
        var reply = ai.chat("You are Pack-Bot.", List.of(
                GeminiClient.userContent("What is EVOH?"),
                GeminiClient.modelContent("A high oxygen barrier."),
                GeminiClient.userContent("Is it recyclable?")));
        assertThat(reply).contains("Hello from Groq");

        JsonNode req = lastRequest.get();
        assertThat(lastAuth.get()).isEqualTo("Bearer test-key");
        assertThat(req.get("model").asText()).isEqualTo("test-model");
        assertThat(req.get("messages")).hasSize(4);
        assertThat(req.get("messages").get(0).get("role").asText()).isEqualTo("system");
        assertThat(req.get("messages").get(2).get("role").asText()).isEqualTo("assistant");
        assertThat(req.get("messages").get(3).get("content").asText()).isEqualTo("Is it recyclable?");
        assertThat(req.has("response_format")).isFalse();
    }

    @Test
    void jsonRequestsUseJsonMode() {
        ai.json("Return JSON only.", "500 g paneer", Map.of());
        assertThat(lastRequest.get().get("response_format").get("type").asText()).isEqualTo("json_object");
    }

    @Test
    void groqErrorFallsThroughToTemplates() {
        status.set(429);
        assertThat(ai.text("system", "hi")).isEmpty();
    }

    @Test
    void noProviderMeansNoAi() {
        GeminiProperties gemini = new GeminiProperties();
        gemini.setApiKey("");
        GroqProperties groq = new GroqProperties();
        groq.setApiKey("");
        GeminiClient none = new GeminiClient(gemini, new GroqClient(groq, json));
        assertThat(none.isEnabled()).isFalse();
        assertThat(none.text("system", "hi")).isEmpty();
        assertThat(lastRequest.get()).isNull();
    }

}
