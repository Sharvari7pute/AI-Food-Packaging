package com.packsmart.service.ai;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import com.google.genai.types.Content;
import com.packsmart.dto.AiDtos.ChatMessage;
import com.packsmart.dto.AiDtos.ChatResponse;
import com.packsmart.dto.CatalogDtos.LaminateDto;
import com.packsmart.dto.CatalogDtos.MaterialDto;
import com.packsmart.dto.RecommendResponse;
import com.packsmart.entity.Commodity;
import com.packsmart.service.CatalogService;
import com.packsmart.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Pack-Bot: a packaging assistant for Indian food MSMEs that answers only from our database
 * (materials, commodities, laminates and optionally the current recommendation). Fallback: FAQ + table lookup.
 */
@Service
@RequiredArgsConstructor
public class ChatService {

    private final GeminiClient gemini;
    private final CatalogService catalog;
    private final RecommendationService recommendations;

    public ChatResponse chat(List<ChatMessage> messages, String language, Long recommendationId) {
        String lang = Languages.normalise(language);
        RecommendResponse current = recommendationId == null ? null : safeGet(recommendationId);
        String system = """
                You are Pack-Bot, a friendly packaging assistant for small Indian food businesses (MSMEs).
                Answer ONLY from the context below. If the answer is not in the context, say you don't know and suggest
                running the PackSmart recommender. Never invent numbers or materials. Keep answers short (under 120 words).
                Reply in %s unless the user clearly writes in another language.

                CONTEXT
                %s
                """.formatted(Languages.name(lang), context(current));
        List<Content> turns = new ArrayList<>();
        for (ChatMessage m : messages) {
            turns.add("assistant".equals(m.role()) ? GeminiClient.modelContent(m.content()) : GeminiClient.userContent(m.content()));
        }
        Optional<String> reply = gemini.chat(system, turns);
        if (reply.isPresent()) {
            return new ChatResponse(reply.get(), true);
        }
        String last = messages.get(messages.size() - 1).content();
        return new ChatResponse(fallback(last, current), false);
    }

    private RecommendResponse safeGet(Long id) {
        try {
            return recommendations.get(id);
        } catch (RuntimeException e) {
            return null;
        }
    }

    /** Compact tables for grounding. */
    String context(RecommendResponse current) {
        StringBuilder sb = new StringBuilder();
        sb.append("MATERIALS (name | type | OTR@25µm cc/m²·day·atm | WVTR@25µm g/m²·day | ₹/kg | min..max °C | recyclable | biodegradable | transparent | heat-sealable | strength/5 | family)\n");
        for (MaterialDto m : catalog.materials()) {
            sb.append(m.name()).append(" | ").append(m.type()).append(" | ").append(m.otr25um()).append(" | ")
                    .append(m.wvtr25um()).append(" | ").append(m.costPerKgInr()).append(" | ").append(m.minTempC())
                    .append("..").append(m.maxTempC()).append(" | ").append(yn(m.recyclable())).append(" | ")
                    .append(yn(m.biodegradable())).append(" | ").append(yn(m.transparent())).append(" | ")
                    .append(yn(m.heatSealable())).append(" | ").append(m.strength()).append(" | ").append(m.family())
                    .append(m.approx() ? " | approx data" : "").append('\n');
        }
        sb.append("\nLAMINATES (name | layers | OTR | WVTR | recyclable | typical use)\n");
        for (LaminateDto l : catalog.laminates()) {
            sb.append(l.name()).append(" | ")
                    .append(String.join(" / ", l.layers().stream().map(x -> x.material() + " " + x.thicknessUm() + "µm").toList()))
                    .append(" | ").append(l.otr()).append(" | ").append(l.wvtr()).append(" | ").append(yn(l.recyclable()))
                    .append(" | ").append(l.typicalUse()).append('\n');
        }
        sb.append("\nFOODS (name | Hindi | category | aw | fat% | O2 sensitivity | light sensitivity | respiring | default shelf life days)\n");
        for (Commodity c : catalog.commodityEntities()) {
            sb.append(c.getName()).append(" | ").append(c.getNameHi()).append(" | ").append(c.getCategory()).append(" | ")
                    .append(c.getWaterActivity()).append(" | ").append(c.getFatPct()).append(" | ").append(c.getO2Sensitive())
                    .append(" | ").append(c.getLightSensitive()).append(" | ").append(yn(c.getRespiring())).append(" | ")
                    .append(c.getDefaultShelfLifeDays()).append('\n');
        }
        sb.append("\nGLOSSARY: OTR = oxygen transmission rate (lower = better oxygen barrier). WVTR = water vapour transmission rate "
                + "(lower = better moisture barrier). aw = water activity. MAP = modified atmosphere packaging for fresh produce.\n");
        if (current != null) {
            sb.append("\nCURRENT RECOMMENDATION\n").append(ExplainService.facts(current));
        }
        return sb.toString();
    }

    private static String yn(Boolean b) {
        return Boolean.TRUE.equals(b) ? "yes" : "no";
    }

    /** Offline answers: material/food lookup from the DB plus a few FAQ entries. */
    String fallback(String question, RecommendResponse current) {
        String q = question.toLowerCase(Locale.ROOT);
        String prefix = "Pack-Bot needs internet/AI key for free-form answers; here is what I found in our database. ";
        for (MaterialDto m : catalog.materials()) {
            if (q.contains(m.name().toLowerCase(Locale.ROOT))) {
                return prefix + m.name() + " (" + m.type() + "): OTR " + m.otr25um() + " cc/m²·day·atm and WVTR "
                        + m.wvtr25um() + " g/m²·day at 25 µm, ₹" + m.costPerKgInr() + "/kg, usable "
                        + m.minTempC() + " to " + m.maxTempC() + " °C, " + (Boolean.TRUE.equals(m.recyclable()) ? "recyclable" : "not recyclable")
                        + ", " + (Boolean.TRUE.equals(m.heatSealable()) ? "heat sealable" : "not heat sealable") + "."
                        + (m.notes() != null ? " Note: " + m.notes() : "");
            }
        }
        for (Commodity c : catalog.commodityEntities()) {
            if (q.contains(c.getName().toLowerCase(Locale.ROOT)) || (c.getNameHi() != null && q.contains(c.getNameHi()))) {
                return prefix + c.getName() + ": water activity " + c.getWaterActivity() + ", fat " + c.getFatPct()
                        + "%, oxygen sensitivity " + c.getO2Sensitive() + ", light sensitivity " + c.getLightSensitive()
                        + (Boolean.TRUE.equals(c.getRespiring()) ? ", fresh produce that needs a breathable film (MAP)" : "")
                        + ". Run the recommender for the exact pack.";
            }
        }
        if (q.contains("otr") && q.contains("wvtr")) {
            return prefix + "OTR (oxygen transmission rate) is how much oxygen passes through a film per m² per day - low OTR "
                    + "stops oily foods going rancid. WVTR (water vapour transmission rate) is how much moisture passes - low WVTR "
                    + "keeps dry foods crisp and moist foods from drying out. A good pack must meet both limits for your food.";
        }
        if (q.contains("wvtr") || q.contains("moisture") || q.contains("nami")) {
            return prefix + "WVTR (water vapour transmission rate) says how much moisture passes through a film per m² per day. "
                    + "Lower WVTR keeps dry foods like chips crisp.";
        }
        if (q.contains("otr") || q.contains("oxygen")) {
            return prefix + "OTR (oxygen transmission rate) says how much oxygen passes through a film per m² per day. "
                    + "Lower OTR stops oily foods from going rancid.";
        }
        if (q.contains("map") || q.contains("fruit") || q.contains("vegetable") || q.contains("sabzi")) {
            return prefix + "Fresh fruits and vegetables keep breathing, so they need a breathable film that holds low O₂ "
                    + "and some CO₂ (modified atmosphere packaging). The recommender picks the film thickness for you.";
        }
        if (q.contains("recycl") || q.contains("eco")) {
            return prefix + "Mono-material packs (all layers from one polymer family, e.g. BOPP/CPP) are recyclable. "
                    + "Aluminium-foil laminates are not. Choose the 'Eco' priority to rank recyclable packs higher.";
        }
        if (current != null && !current.options().isEmpty()) {
            var top = current.options().get(0);
            return prefix + "For your current result (" + current.commodity() + ") the top pack is " + top.name()
                    + " with about " + top.estimatedShelfLifeDays() + " days shelf life.";
        }
        return prefix + "Materials we know: " + String.join(", ", catalog.materials().stream().map(MaterialDto::name).toList())
                + ". Ask about any of them, or about OTR, WVTR, MAP or recyclability.";
    }
}
