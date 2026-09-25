package com.packsmart.service.ai;

import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.packsmart.dto.AiDtos.ParseResponse;
import com.packsmart.dto.CatalogDtos.CityDto;
import com.packsmart.dto.RecommendRequest;
import com.packsmart.entity.Commodity;
import com.packsmart.service.CatalogService;
import com.packsmart.service.engine.model.StorageType;
import com.packsmart.service.engine.model.Transport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Natural language (English / Hindi / Hinglish / Marathi) → a draft {@link RecommendRequest}.
 * Gemini only fills form fields; the food must map to one of our commodity names. Fallback: keyword matcher.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QueryParserService {

    private static final Pattern WEIGHT = Pattern.compile(
            "(\\d+(?:[.,]\\d+)?)\\s*(kgs?|kilos?|kilograms?|किलो|gms?|grams?|gramm?e?s?|ग्राम|g)(?![a-z])",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern DAYS = Pattern.compile(
            "(\\d+)\\s*(din|dino|dinon|days?|दिन|दिवस|diwas|divas)(?![a-z])", Pattern.CASE_INSENSITIVE);
    private static final Pattern WEEKS = Pattern.compile(
            "(\\d+)\\s*(hafte|haftey|hafta|weeks?|हफ्ते|हफ्ता|आठवडे)(?![a-z])", Pattern.CASE_INSENSITIVE);
    private static final Pattern MONTHS = Pattern.compile(
            "(\\d+)\\s*(mahine|mahina|months?|महीने|महीना|महिने)(?![a-z])", Pattern.CASE_INSENSITIVE);
    private static final Pattern TEMP = Pattern.compile(
            "(-?\\d+(?:\\.\\d+)?)\\s*(°\\s*c?|degrees?|deg|डिग्री)", Pattern.CASE_INSENSITIVE);
    private static final Pattern RH = Pattern.compile(
            "(\\d+(?:\\.\\d+)?)\\s*%\\s*(rh|humidity|nami|नमी)?", Pattern.CASE_INSENSITIVE);
    private static final Pattern FROZEN_WORDS = Pattern.compile("freez|frozen|froze|फ्रीज़र|जमा", Pattern.CASE_INSENSITIVE);
    private static final Pattern CHILLED_WORDS = Pattern.compile("fridge|refrigerat|chill|thanda|ठंडा|फ्रिज", Pattern.CASE_INSENSITIVE);
    private static final Pattern AMBIENT_WORDS = Pattern.compile("room temp|ambient|shelf|bahar|normal temp|कमरे", Pattern.CASE_INSENSITIVE);
    private static final Pattern LONG_WORDS = Pattern.compile("long distance|export|door bhej|dur bhej|dusre shahar|दूसरे शहर|courier|ship",
            Pattern.CASE_INSENSITIVE);
    private static final double GRAMS_PER_KG = 1000;
    private static final int DAYS_PER_WEEK = 7;
    private static final int DAYS_PER_MONTH = 30;

    private final GeminiClient gemini;
    private final CatalogService catalog;
    private final ObjectMapper json;

    public ParseResponse parse(String query) {
        Draft draft = gemini.isEnabled() ? aiParse(query).orElse(null) : null;
        boolean aiUsed = draft != null;
        if (draft == null) {
            draft = keywordParse(query);
        }
        List<String> notes = new ArrayList<>();
        applyCity(query, draft, notes);

        Long commodityId = null;
        String commodityName = null;
        if (draft.commodity != null) {
            Optional<Commodity> c = catalog.commodityByName(draft.commodity);
            if (c.isPresent()) {
                commodityId = c.get().getId();
                commodityName = c.get().getName();
            }
        }
        RecommendRequest req = new RecommendRequest(commodityId, null, null, draft.weightG, null, draft.days,
                draft.storage, draft.tempC, draft.rh, draft.transport, null, null);
        List<String> missing = new ArrayList<>();
        if (commodityId == null) {
            missing.add("commodityId");
            notes.add("Food not recognised - pick it from the list or use \"Food not in list?\".");
        }
        if (draft.weightG == null) {
            missing.add("packWeightG");
        }
        if (draft.days == null) {
            missing.add("shelfLifeDays");
        }
        if (draft.storage == null) {
            missing.add("storageType");
        }
        if (draft.tempC == null) {
            missing.add("storageTempC");
        }
        if (draft.rh == null) {
            missing.add("relativeHumidityPct");
        }
        return new ParseResponse(req, commodityName, missing, aiUsed, notes.isEmpty() ? null : String.join(" ", notes));
    }

    /** Mutable parse result. */
    static final class Draft {
        String commodity;
        Double weightG;
        Integer days;
        StorageType storage;
        Double tempC;
        Double rh;
        Transport transport;
    }

    // ------------------------------------------------------------------ Gemini path

    private Optional<Draft> aiParse(String query) {
        List<String> names = catalog.commodityEntities().stream()
                .map(c -> c.getName() + (c.getNameHi() != null ? " (" + c.getNameHi() + ")" : ""))
                .toList();
        String system = """
                You convert an Indian food business owner's message (English, Hindi, Hinglish or Marathi) into form fields
                for a packaging tool. Return JSON only. Map the food to EXACTLY one name from this list, or omit
                "commodity" if nothing matches: %s.
                Rules: packWeightG in grams; shelfLifeDays in days (1 week = 7, 1 month = 30);
                storageType AMBIENT (room/shelf), CHILLED (fridge) or FROZEN (freezer); storageTempC only if a temperature
                is stated; relativeHumidityPct only if humidity is stated; transport LONG_DISTANCE only if shipping far,
                else LOCAL. Omit any field that is not in the message. Never guess numbers.
                """.formatted(String.join(", ", names));
        Map<String, Object> schema = Map.of(
                "type", "object",
                "properties", Map.of(
                        "commodity", Map.of("type", "string"),
                        "packWeightG", Map.of("type", "number"),
                        "shelfLifeDays", Map.of("type", "integer"),
                        "storageType", Map.of("type", "string", "enum", List.of("AMBIENT", "CHILLED", "FROZEN")),
                        "storageTempC", Map.of("type", "number"),
                        "relativeHumidityPct", Map.of("type", "number"),
                        "transport", Map.of("type", "string", "enum", List.of("LOCAL", "LONG_DISTANCE"))));
        return gemini.json(system, query, schema).flatMap(text -> {
            try {
                JsonNode n = json.readTree(text);
                Draft d = new Draft();
                d.commodity = textOrNull(n, "commodity");
                d.weightG = inRange(numOrNull(n, "packWeightG"), 1, 50000);
                Double days = inRange(numOrNull(n, "shelfLifeDays"), 1, 730);
                d.days = days == null ? null : (int) Math.round(days);
                d.storage = enumOrNull(StorageType.class, textOrNull(n, "storageType"));
                d.tempC = inRange(numOrNull(n, "storageTempC"), -40, 60);
                d.rh = inRange(numOrNull(n, "relativeHumidityPct"), 0, 100);
                d.transport = enumOrNull(Transport.class, textOrNull(n, "transport"));
                return Optional.of(d);
            } catch (Exception e) {
                log.warn("Could not read Gemini parse output, using keyword fallback: {}", e.getMessage());
                return Optional.empty();
            }
        });
    }

    private static String textOrNull(JsonNode n, String f) {
        JsonNode v = n.get(f);
        return v == null || v.isNull() || v.asText().isBlank() ? null : v.asText();
    }

    private static Double numOrNull(JsonNode n, String f) {
        JsonNode v = n.get(f);
        return v == null || !v.isNumber() ? null : v.asDouble();
    }

    private static Double inRange(Double v, double min, double max) {
        return v == null || v < min || v > max ? null : v;
    }

    private static <E extends Enum<E>> E enumOrNull(Class<E> type, String v) {
        if (v == null) {
            return null;
        }
        try {
            return Enum.valueOf(type, v.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    // ------------------------------------------------------------------ keyword fallback

    /** Keyword matcher used without Gemini: commodity names (English/Hindi), numbers with units, storage words. */
    Draft keywordParse(String query) {
        String q = query.toLowerCase(Locale.ROOT);
        Draft d = new Draft();
        d.commodity = catalog.commodityEntities().stream()
                .filter(c -> matches(q, c.getName()) || (c.getNameHi() != null && q.contains(c.getNameHi())))
                .max(Comparator.comparingInt(c -> c.getName().length()))
                .map(Commodity::getName)
                .orElse(null);

        Matcher w = WEIGHT.matcher(q);
        if (w.find()) {
            double v = Double.parseDouble(w.group(1).replace(',', '.'));
            String unit = w.group(2);
            d.weightG = unit.startsWith("k") || unit.startsWith("कि") ? v * GRAMS_PER_KG : v;
        }
        Matcher m;
        if ((m = DAYS.matcher(q)).find()) {
            d.days = Integer.parseInt(m.group(1));
        } else if ((m = WEEKS.matcher(q)).find()) {
            d.days = Integer.parseInt(m.group(1)) * DAYS_PER_WEEK;
        } else if ((m = MONTHS.matcher(q)).find()) {
            d.days = Integer.parseInt(m.group(1)) * DAYS_PER_MONTH;
        }
        if (FROZEN_WORDS.matcher(q).find()) {
            d.storage = StorageType.FROZEN;
        } else if (CHILLED_WORDS.matcher(q).find()) {
            d.storage = StorageType.CHILLED;
        } else if (AMBIENT_WORDS.matcher(q).find()) {
            d.storage = StorageType.AMBIENT;
        }
        if ((m = TEMP.matcher(q)).find()) {
            d.tempC = inRange(Double.parseDouble(m.group(1)), -40, 60);
        }
        if ((m = RH.matcher(q)).find()) {
            d.rh = inRange(Double.parseDouble(m.group(1)), 0, 100);
        }
        d.transport = LONG_WORDS.matcher(q).find() ? Transport.LONG_DISTANCE : null;
        return d;
    }

    private static boolean matches(String q, String name) {
        String n = name.toLowerCase(Locale.ROOT);
        return Pattern.compile("(?<![a-z])" + Pattern.quote(n) + "s?(?![a-z])").matcher(q).find();
    }

    /** A city name in the text fills temperature and RH for the current season (only if not stated). */
    private void applyCity(String query, Draft d, List<String> notes) {
        String q = query.toLowerCase(Locale.ROOT);
        for (CityDto c : catalog.cities()) {
            if (!matches(q, c.name())) {
                continue;
            }
            String season = season(LocalDate.now().getMonth());
            Map<String, Double[]> bySeason = new LinkedHashMap<>();
            bySeason.put("summer", new Double[]{c.summerTempC(), c.summerRhPct()});
            bySeason.put("monsoon", new Double[]{c.monsoonTempC(), c.monsoonRhPct()});
            bySeason.put("winter", new Double[]{c.winterTempC(), c.winterRhPct()});
            Double[] v = bySeason.get(season);
            boolean ambient = d.storage == null || d.storage == StorageType.AMBIENT;
            if (d.tempC == null && ambient && v[0] != null) {
                d.tempC = v[0];
            }
            if (d.rh == null && v[1] != null) {
                d.rh = v[1];
            }
            notes.add("Climate filled from " + c.name() + " (" + season + ").");
            return;
        }
    }

    static String season(Month month) {
        return switch (month) {
            case MARCH, APRIL, MAY -> "summer";
            case JUNE, JULY, AUGUST, SEPTEMBER -> "monsoon";
            default -> "winter";
        };
    }
}
