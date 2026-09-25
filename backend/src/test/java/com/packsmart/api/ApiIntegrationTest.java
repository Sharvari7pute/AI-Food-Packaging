package com.packsmart.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Files;
import java.nio.file.Path;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.packsmart.repository.CommodityRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/** End-to-end API tests on an H2 database seeded from the same CSV files. Gemini is disabled (fallbacks). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiIntegrationTest {

    @Autowired
    MockMvc mvc;
    @Autowired
    ObjectMapper json;
    @Autowired
    CommodityRepository commodities;

    private long id(String name) {
        return commodities.findByNameIgnoreCase(name).orElseThrow().getId();
    }

    private String request(String food, double weight, int days, String storage, double temp, double rh) {
        return """
                {"commodityId": %d, "packWeightG": %s, "shelfLifeDays": %d, "storageType": "%s",
                 "storageTempC": %s, "relativeHumidityPct": %s, "transport": "LOCAL", "priority": "DEFAULT", "language": "en"}
                """.formatted(id(food), weight, days, storage, temp, rh);
    }

    private JsonNode recommend(String body) throws Exception {
        MvcResult res = mvc.perform(post("/api/recommend").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andReturn();
        return json.readTree(res.getResponse().getContentAsString());
    }

    @Test
    void healthReportsAiDisabledWithoutKey() throws Exception {
        mvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"))
                .andExpect(jsonPath("$.aiEnabled").value(false));
    }

    @Test
    void catalogEndpointsServeSeededData() throws Exception {
        mvc.perform(get("/api/commodities")).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(25)))
                .andExpect(jsonPath("$[0].nameHi").value("चिप्स"));
        mvc.perform(get("/api/commodities/" + id("Paneer"))).andExpect(status().isOk())
                .andExpect(jsonPath("$.waterActivity").value(0.973))
                .andExpect(jsonPath("$.mainDeteriorationFactor").value("microbial_spoilage"))
                .andExpect(jsonPath("$.storageTempMinC").value(3.0));
        mvc.perform(get("/api/materials")).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(16)))
                .andExpect(jsonPath("$[0].name").value("LDPE"))
                .andExpect(jsonPath("$[0].approx").value(true))
                .andExpect(jsonPath("$[0].family").value("PE"));
        mvc.perform(get("/api/laminates")).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(7)))
                .andExpect(jsonPath("$[0].otr", notNullValue()));
        mvc.perform(get("/api/cities")).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(9)));
        mvc.perform(get("/api/commodities/99999")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void chipsRecommendationEndToEnd() throws Exception {
        JsonNode r = recommend(request("Chips", 100, 90, "AMBIENT", 30, 70));
        assertThat(r.get("id").asLong()).isPositive();
        assertThat(r.get("inputs").get("mainDeteriorationFactor").asText()).isEqualTo("oxidative_rancidity_and_moisture_uptake");
        assertThat(r.get("inputs").get("recommendedStorageTempMaxC").asDouble()).isEqualTo(25.0);
        assertThat(r.get("requirements").get("o2Barrier").asText()).isEqualTo("HIGH");
        assertThat(r.get("requirements").get("moistureMode").asText()).isEqualTo("KEEP_OUT");
        assertThat(r.get("requirements").get("opaque").asBoolean()).isTrue();
        assertThat(r.get("options").get(0).get("layers").toString()).contains("Aluminium foil");
        assertThat(r.get("avoid").get("name").asText()).isEqualTo("LDPE");
        double reqOtr = r.get("requiredOtr").asDouble();
        for (JsonNode o : r.get("options")) {
            assertThat(o.get("otr").asDouble()).isLessThanOrEqualTo(reqOtr);
            assertThat(o.get("curve").size()).isBetween(1, 60);
        }
        String shareId = r.get("shareId").asText();

        mvc.perform(get("/api/recommendations/" + r.get("id").asLong())).andExpect(status().isOk())
                .andExpect(jsonPath("$.commodity").value("Chips"));
        mvc.perform(get("/api/recommendations/share/" + shareId)).andExpect(status().isOk())
                .andExpect(jsonPath("$.shareId").value(shareId));
        mvc.perform(get("/api/recommendations")).andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].commodity", hasItem("Chips")));

        MvcResult pdf = mvc.perform(get("/api/report/" + shareId + "/pdf")).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF)).andReturn();
        byte[] bytes = pdf.getResponse().getContentAsByteArray();
        assertThat(new String(bytes, 0, 5)).isEqualTo("%PDF-");
        String out = System.getProperty("packsmart.samplePdf");
        if (out != null) {
            Files.write(Path.of(out), bytes);
        }
        mvc.perform(get("/api/report/" + shareId + "/qr")).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG));
        mvc.perform(get("/api/report/does-not-exist/pdf")).andExpect(status().isNotFound());
    }

    @Test
    void tomatoGetsMapPanel() throws Exception {
        JsonNode r = recommend(request("Tomato", 500, 7, "CHILLED", 12, 90));
        assertThat(r.get("requirements").get("needsMap").asBoolean()).isTrue();
        JsonNode map = r.get("map");
        assertThat(map.get("targetO2Min").asDouble()).isEqualTo(3);
        assertThat(map.get("targetO2Max").asDouble()).isEqualTo(5);
        assertThat(map.get("perforationNeeded").asBoolean() || map.get("filmOtr").asDouble() >= map.get("requiredOtr").asDouble())
                .isTrue();
    }

    @Test
    void validationErrorsReturn400WithDetails() throws Exception {
        mvc.perform(post("/api/recommend").contentType(MediaType.APPLICATION_JSON)
                        .content(request("Chips", 100, 0, "AMBIENT", 30, 70)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation failed"))
                .andExpect(jsonPath("$.details[0]", containsString("shelfLifeDays")));
        mvc.perform(post("/api/recommend").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"packWeightG\":100,\"shelfLifeDays\":10,\"storageType\":\"AMBIENT\",\"storageTempC\":30,\"relativeHumidityPct\":60}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/recommend").contentType(MediaType.APPLICATION_JSON)
                        .content(request("Chips", 100, 10, "HOT", 30, 70)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void simulateDoesNotSave() throws Exception {
        mvc.perform(post("/api/simulate").contentType(MediaType.APPLICATION_JSON)
                        .content(request("Atta", 1000, 90, "AMBIENT", 40, 70)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", nullValue()))
                .andExpect(jsonPath("$.shareId", nullValue()))
                .andExpect(jsonPath("$.requiredWvtr", notNullValue()))
                .andExpect(jsonPath("$.options[0].name", notNullValue()));
    }

    @Test
    void customFoodRunsWithoutDatabaseRow() throws Exception {
        String body = """
                {"customCommodity": {"name": "Peanut chikki", "category": "dry_snack", "waterActivity": 0.3, "fatPct": 30,
                  "o2Sensitive": "high", "lightSensitive": "medium", "respiring": false, "aiEstimated": true},
                 "packWeightG": 200, "shelfLifeDays": 60, "storageType": "AMBIENT", "storageTempC": 30, "relativeHumidityPct": 65}
                """;
        mvc.perform(post("/api/simulate").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.commodity").value("Peanut chikki"))
                .andExpect(jsonPath("$.aiEstimatedFood").value(true))
                .andExpect(jsonPath("$.requirements.o2Barrier").value("HIGH"));
    }

    @Test
    void laminateBuilderEvaluatesAndTestsAgainstFood() throws Exception {
        String body = """
                {"layers": [{"material": "BOPP", "thicknessUm": 20}, {"material": "CPP", "thicknessUm": 30}],
                 "test": {"commodityId": %d, "packWeightG": 100, "shelfLifeDays": 90, "storageType": "AMBIENT",
                          "storageTempC": 30, "relativeHumidityPct": 70}}
                """.formatted(id("Chips"));
        mvc.perform(post("/api/laminates/evaluate").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recyclable").value(true))
                .andExpect(jsonPath("$.family").value("PP"))
                .andExpect(jsonPath("$.test.passes").value(false))
                .andExpect(jsonPath("$.test.failReasons[0]", containsString("OTR")));
        mvc.perform(post("/api/laminates/evaluate").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"layers\":[{\"material\":\"Kryptonite\",\"thicknessUm\":10}]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void aiParseFallsBackToKeywordMatcher() throws Exception {
        mvc.perform(post("/api/ai/parse").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\": \"500 g paneer 10 din fridge\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aiUsed").value(false))
                .andExpect(jsonPath("$.commodityName").value("Paneer"))
                .andExpect(jsonPath("$.draft.commodityId").value(id("Paneer")))
                .andExpect(jsonPath("$.draft.packWeightG").value(500.0))
                .andExpect(jsonPath("$.draft.shelfLifeDays").value(10))
                .andExpect(jsonPath("$.draft.storageType").value("CHILLED"));
        mvc.perform(post("/api/ai/parse").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\": \"mujhe 2 kg atta 3 mahine Nagpur mein rakhna hai\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.commodityName").value("Atta"))
                .andExpect(jsonPath("$.draft.packWeightG").value(2000.0))
                .andExpect(jsonPath("$.draft.shelfLifeDays").value(90))
                .andExpect(jsonPath("$.draft.relativeHumidityPct", notNullValue()));
    }

    @Test
    void aiExplainChatAndEstimateFallBackWithoutKey() throws Exception {
        JsonNode r = recommend(request("Paneer", 200, 7, "CHILLED", 4, 70));
        mvc.perform(post("/api/ai/explain").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"recommendationId\": " + r.get("id").asLong() + ", \"language\": \"hi\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aiUsed").value(false))
                .andExpect(jsonPath("$.language").value("hi"))
                .andExpect(jsonPath("$.text", containsString("Paneer")));
        mvc.perform(post("/api/ai/chat").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"messages\": [{\"role\": \"user\", \"content\": \"What is EVOH?\"}], \"language\": \"en\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aiUsed").value(false))
                .andExpect(jsonPath("$.reply", containsString("EVOH")));
        mvc.perform(post("/api/ai/estimate-food").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Dragon fruit jam\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error", containsString("closest food")));
        mvc.perform(post("/api/ai/estimate-food").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"paneer\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.existingCommodityId").value(id("Paneer")));
    }

    @Test
    void corsAllowsLocalFrontend() throws Exception {
        mvc.perform(get("/api/health").header("Origin", "http://localhost:3000"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"));
    }
}
