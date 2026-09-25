package com.packsmart.service.ai;

import java.util.List;
import java.util.stream.Collectors;

import com.packsmart.dto.AiDtos.ExplainResponse;
import com.packsmart.dto.RecommendResponse;
import com.packsmart.dto.RecommendResponse.OptionDto;
import com.packsmart.entity.Recommendation;
import com.packsmart.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Plain-language explanation of an engine result (en / hi / mr) for a small business owner.
 * The prompt contains ONLY engine output; Gemini must not add numbers or materials. Fallback: a template.
 */
@Service
@RequiredArgsConstructor
public class ExplainService {

    private static final int MAX_REASONS = 4;

    private final GeminiClient gemini;
    private final RecommendationService recommendations;

    public ExplainResponse explain(Long recommendationId, String shareId, String language) {
        String lang = Languages.normalise(language);
        Recommendation rec = recommendationId != null
                ? recommendations.entity(recommendationId)
                : recommendations.entityByShareId(shareId == null ? "" : shareId);
        // Re-use a stored AI explanation in the same language: saves Gemini quota and answers instantly.
        if (Boolean.TRUE.equals(rec.getExplanationAiUsed()) && lang.equals(rec.getExplanationLanguage())
                && rec.getExplanation() != null) {
            return new ExplainResponse(rec.getExplanation(), lang, true);
        }
        RecommendResponse r = recommendations.read(rec);

        String facts = facts(r);
        String system = """
                You explain food packaging recommendations to a small Indian food business owner.
                Write 3 to 5 short sentences in %s. Use only these numbers. Do not add new numbers or materials.
                Say which pack to use and why, how long the food should stay fresh, what limits it, and what to avoid.
                No markdown, no bullet points.
                """.formatted(Languages.name(lang));
        ExplainResponse resp = gemini.text(system, facts)
                .map(t -> new ExplainResponse(t, lang, true))
                .orElseGet(() -> new ExplainResponse(template(r, lang), lang, false));
        if (resp.aiUsed() || !Boolean.TRUE.equals(rec.getExplanationAiUsed())) {
            recommendations.saveExplanation(rec.getId(), resp.text(), lang, resp.aiUsed());
        }
        return resp;
    }

    /** Engine facts sent to Gemini (and nothing else). */
    static String facts(RecommendResponse r) {
        StringBuilder sb = new StringBuilder();
        sb.append("Food: ").append(r.commodity()).append('\n');
        var in = r.inputs();
        sb.append("Pack: ").append(fmt(in.packWeightG())).append(" g, wanted shelf life ").append(in.shelfLifeDays())
                .append(" days, storage ").append(in.storageType()).append(" at ").append(fmt(in.storageTempC()))
                .append(" °C, ").append(fmt(in.relativeHumidityPct())).append("% RH\n");
        sb.append("Requirements: oxygen barrier ").append(r.requirements().o2Barrier()).append(", moisture ")
                .append(r.requirements().moistureMode()).append(r.requirements().opaque() ? ", opaque" : "").append('\n');
        sb.append("Reasons: ").append(String.join(" | ", r.requirements().reasons())).append('\n');
        if (!r.options().isEmpty()) {
            OptionDto top = r.options().get(0);
            sb.append("Top option: ").append(top.name()).append(" (").append(layers(top)).append("), total ")
                    .append(fmt(top.totalThicknessUm())).append(" µm, estimated shelf life ")
                    .append(top.estimatedShelfLifeDays()).append(" days, limited by ").append(top.limitingFactor())
                    .append(", cost ₹").append(fmt(top.costPer1000Inr())).append(" per 1000 packs")
                    .append(top.recyclable() ? ", recyclable" : ", not recyclable").append('\n');
        } else {
            sb.append("No option met every requirement.\n");
        }
        if (r.map() != null) {
            sb.append("Modified atmosphere: film ").append(r.map().film()).append(' ').append(r.map().filmThicknessUm())
                    .append(" µm").append(r.map().perforationNeeded() ? " with micro-perforations" : "").append('\n');
        }
        if (r.avoid() != null) {
            sb.append("Avoid: ").append(r.avoid().name()).append(" because ").append(r.avoid().reason()).append('\n');
        }
        return sb.toString();
    }

    private static String layers(OptionDto o) {
        return o.layers().stream().map(l -> l.material() + " " + fmt(l.thicknessUm()) + " µm").collect(Collectors.joining(" / "));
    }

    private static String fmt(double v) {
        return v == Math.rint(v) ? String.valueOf((long) v) : String.valueOf(v);
    }

    /** Non-AI explanation built only from the engine's reasons and numbers. */
    static String template(RecommendResponse r, String lang) {
        // Keep the plain-language rules; the formula lines are already shown in the decision trace.
        List<String> reasons = r.requirements().reasons().stream()
                .filter(x -> !x.contains("required OTR") && !x.contains("required WVTR") && !x.contains("film OTR must"))
                .limit(MAX_REASONS).toList();
        String why = String.join(". ", reasons) + ".";
        OptionDto top = r.options().isEmpty() ? null : r.options().get(0);
        String avoidName = r.avoid() != null ? r.avoid().name() : null;
        return switch (lang) {
            case "hi" -> (top == null
                    ? r.commodity() + " के लिए कोई भी पैक सभी शर्तें पूरी नहीं करता — नीचे के विकल्प देखें। "
                    : r.commodity() + " के लिए सबसे अच्छा पैक " + top.name() + " (" + layers(top) + ") है। यह लगभग "
                    + top.estimatedShelfLifeDays() + " दिन तक ताज़ा रखेगा। लागत ₹" + fmt(top.costPer1000Inr())
                    + " प्रति 1000 पैक। ")
                    + (avoidName != null ? avoidName + " का उपयोग न करें। " : "") + "कारण: " + why;
            case "mr" -> (top == null
                    ? r.commodity() + " साठी कोणताही पॅक सर्व अटी पूर्ण करत नाही — खालील पर्याय पहा. "
                    : r.commodity() + " साठी सर्वोत्तम पॅक " + top.name() + " (" + layers(top) + ") आहे. तो सुमारे "
                    + top.estimatedShelfLifeDays() + " दिवस ताजा ठेवेल. खर्च ₹" + fmt(top.costPer1000Inr())
                    + " प्रति 1000 पॅक. ")
                    + (avoidName != null ? avoidName + " वापरू नका. " : "") + "कारण: " + why;
            default -> (top == null
                    ? "No pack meets every requirement for " + r.commodity() + " - see the near misses below. "
                    : "For " + r.commodity() + ", the best pack is " + top.name() + " (" + layers(top) + "). It should keep the food fresh for about "
                    + top.estimatedShelfLifeDays() + " days" + (top.limitingFactor().equals("NONE") ? "" : ", limited by "
                    + top.limitingFactor().toLowerCase()) + ". Cost is about ₹" + fmt(top.costPer1000Inr()) + " per 1000 packs. ")
                    + "Why: " + why
                    + (r.avoid() != null ? " Avoid " + avoidName + " - it does not meet these needs." : "");
        };
    }
}
