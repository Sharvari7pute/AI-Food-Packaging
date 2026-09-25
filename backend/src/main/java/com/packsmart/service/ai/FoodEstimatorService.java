package com.packsmart.service.ai;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.packsmart.dto.AiDtos.EstimateFoodResponse;
import com.packsmart.dto.CustomCommodity;
import com.packsmart.entity.Commodity;
import com.packsmart.exception.NotFoundException;
import com.packsmart.service.CatalogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Estimates the properties of a food that is not in our database. Always labelled "AI-estimated — verify";
 * never saved. Without AI → 404 asking the user to pick the closest food.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FoodEstimatorService {

    private static final List<String> LEVELS = List.of("high", "medium", "low");

    private final GeminiClient gemini;
    private final CatalogService catalog;
    private final ObjectMapper json;

    public EstimateFoodResponse estimate(String name) {
        String n = name.trim();
        Optional<Commodity> existing = catalog.commodityEntities().stream()
                .filter(c -> c.getName().equalsIgnoreCase(n) || n.equals(c.getNameHi()))
                .findFirst();
        if (existing.isPresent()) {
            Commodity c = existing.get();
            CustomCommodity food = new CustomCommodity(c.getName(), c.getNameHi(), c.getCategory(), c.getMoisturePct(),
                    c.getWaterActivity(), c.getCriticalAw(), c.getFatPct(), c.getO2Sensitive(), c.getLightSensitive(),
                    c.getRespiring(), c.getRespirationRate(), c.getDefaultShelfLifeDays(), false, c.getPh(),
                    c.getStorageTempMinC(), c.getStorageTempMaxC(), c.getMainDeteriorationFactor());
            return new EstimateFoodResponse(food, c.getId(), false, false, "This food is already in our database.");
        }
        String system = """
                You are a food science assistant. Estimate typical physical properties of the food the user names,
                as sold by small Indian food businesses. Return JSON only. Use typical literature values.
                o2Sensitive/lightSensitive are high, medium or low. respiring = true only for fresh fruits/vegetables,
                then give respirationRate in mL O2/(kg·h) at 20 °C. category is one of: dry_snack, bakery, staple, dairy,
                frozen, fruit, vegetable, meat, beverage, other.
                Return one flat JSON object with these keys: name, nameHi, category, moisturePct, waterActivity (0-1),
                criticalAw, fatPct, o2Sensitive, lightSensitive, respiring (true/false), respirationRate (only if respiring),
                defaultShelfLifeDays, mainDeteriorationFactor (short snake_case phrase, e.g. oxidative_rancidity).
                """;
        Map<String, Object> num = Map.of("type", "number");
        Map<String, Object> level = Map.of("type", "string", "enum", LEVELS);
        Map<String, Object> schema = Map.of(
                "type", "object",
                "properties", Map.ofEntries(
                        Map.entry("name", Map.of("type", "string")),
                        Map.entry("nameHi", Map.of("type", "string")),
                        Map.entry("category", Map.of("type", "string")),
                        Map.entry("moisturePct", num),
                        Map.entry("waterActivity", num),
                        Map.entry("criticalAw", num),
                        Map.entry("fatPct", num),
                        Map.entry("o2Sensitive", level),
                        Map.entry("lightSensitive", level),
                        Map.entry("respiring", Map.of("type", "boolean")),
                        Map.entry("respirationRate", num),
                        Map.entry("defaultShelfLifeDays", Map.of("type", "integer")),
                        Map.entry("mainDeteriorationFactor", Map.of("type", "string"))),
                "required", List.of("name", "category", "moisturePct", "waterActivity", "fatPct", "o2Sensitive",
                        "lightSensitive", "respiring", "defaultShelfLifeDays"));
        return gemini.json(system, "Food: " + n, schema)
                .flatMap(text -> read(text, n))
                .map(food -> new EstimateFoodResponse(food, null, true, true,
                        "AI-estimated — verify these values before relying on them."))
                .orElseThrow(() -> new NotFoundException(
                        "\"" + n + "\" is not in our database and AI estimation is not available right now. "
                                + "Please pick the closest food from the list."));
    }

    private Optional<CustomCommodity> read(String text, String requested) {
        try {
            JsonNode n = json.readTree(text);
            double aw = clamp(n.path("waterActivity").asDouble(Double.NaN), 0, 1);
            double fat = clamp(n.path("fatPct").asDouble(Double.NaN), 0, 100);
            if (Double.isNaN(aw) || Double.isNaN(fat)) {
                return Optional.empty();
            }
            boolean respiring = n.path("respiring").asBoolean(false);
            Double rr = n.hasNonNull("respirationRate") ? clamp(n.get("respirationRate").asDouble(), 0, 1000) : null;
            return Optional.of(new CustomCommodity(
                    n.hasNonNull("name") ? n.get("name").asText() : requested,
                    n.hasNonNull("nameHi") ? n.get("nameHi").asText() : null,
                    n.path("category").asText("other"),
                    n.hasNonNull("moisturePct") ? clamp(n.get("moisturePct").asDouble(), 0, 100) : null,
                    aw,
                    n.hasNonNull("criticalAw") ? clamp(n.get("criticalAw").asDouble(), 0, 1) : null,
                    fat,
                    level(n.path("o2Sensitive").asText()),
                    level(n.path("lightSensitive").asText()),
                    respiring,
                    respiring ? rr : null,
                    n.hasNonNull("defaultShelfLifeDays") ? Math.max(1, Math.min(730, n.get("defaultShelfLifeDays").asInt())) : null,
                    true, null, null, null,
                    n.hasNonNull("mainDeteriorationFactor") ? n.get("mainDeteriorationFactor").asText() : null));
        } catch (Exception e) {
            log.warn("Could not read Gemini food estimate: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private static String level(String v) {
        String l = v == null ? "" : v.toLowerCase(Locale.ROOT);
        return LEVELS.contains(l) ? l : "medium";
    }

    private static double clamp(double v, double min, double max) {
        return Double.isNaN(v) ? v : Math.max(min, Math.min(max, v));
    }
}
