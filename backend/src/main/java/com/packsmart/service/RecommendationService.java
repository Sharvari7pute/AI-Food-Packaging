package com.packsmart.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.packsmart.dto.HistoryDtos.PageDto;
import com.packsmart.dto.HistoryDtos.RecommendationSummary;
import com.packsmart.dto.LaminateEvaluateRequest;
import com.packsmart.dto.LaminateEvaluateResponse;
import com.packsmart.dto.LaminateEvaluateResponse.FoodTestResult;
import com.packsmart.dto.RecommendRequest;
import com.packsmart.dto.RecommendResponse;
import com.packsmart.entity.Recommendation;
import com.packsmart.exception.NotFoundException;
import com.packsmart.repository.RecommendationRepository;
import com.packsmart.service.engine.BarrierCalculator;
import com.packsmart.service.engine.LaminateService;
import com.packsmart.service.engine.Num;
import com.packsmart.service.engine.PackEconomics;
import com.packsmart.service.engine.RecommendationEngine;
import com.packsmart.service.engine.model.Candidate;
import com.packsmart.service.engine.model.Catalog;
import com.packsmart.service.engine.model.Conditions;
import com.packsmart.service.engine.model.Economics;
import com.packsmart.service.engine.model.EngineResult;
import com.packsmart.service.engine.model.FoodProfile;
import com.packsmart.service.engine.model.LaminateEval;
import com.packsmart.service.engine.model.Layer;
import com.packsmart.service.engine.model.Priority;
import com.packsmart.service.engine.model.Transport;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Runs the engine and stores/reads saved recommendations. */
@Service
@RequiredArgsConstructor
public class RecommendationService {

    private static final int MAX_PAGE_SIZE = 100;

    private final RecommendationEngine engine;
    private final CatalogService catalog;
    private final ResponseMapper mapper;
    private final RecommendationRepository repo;
    private final ObjectMapper json;
    private final LaminateService laminates;
    private final PackEconomics economics;
    private final BarrierCalculator barrier;

    /** Runs the engine and saves the result with a public share id. */
    @Transactional
    public RecommendResponse recommend(RecommendRequest req) {
        EngineResult result = run(req);
        Recommendation rec = new Recommendation();
        rec.setShareId(UUID.randomUUID().toString());
        rec.setCreatedAt(Instant.now());
        rec.setCommodityName(result.food().name());
        rec.setRequestJson(write(req));
        rec = repo.save(rec);
        RecommendResponse resp = mapper.toResponse(result, req.languageOrDefault(), rec.getId(), rec.getShareId(),
                rec.getCreatedAt());
        rec.setResponseJson(write(resp));
        if (!resp.options().isEmpty()) {
            rec.setTopOptionName(resp.options().get(0).name());
            rec.setEstimatedShelfLifeDays(resp.options().get(0).estimatedShelfLifeDays());
        }
        repo.save(rec);
        return resp;
    }

    /** What-if: same pipeline, nothing saved. */
    public RecommendResponse simulate(RecommendRequest req) {
        return mapper.toResponse(run(req), req.languageOrDefault(), null, null, null);
    }

    private EngineResult run(RecommendRequest req) {
        FoodProfile food = engine.resolveFood(req);
        Conditions cond = engine.resolveConditions(req);
        return engine.compute(food, cond, catalog.catalog());
    }

    @Transactional(readOnly = true)
    public RecommendResponse get(Long id) {
        return read(entity(id));
    }

    @Transactional(readOnly = true)
    public RecommendResponse getByShareId(String shareId) {
        return read(entityByShareId(shareId));
    }

    public Recommendation entity(Long id) {
        return repo.findById(id).orElseThrow(() -> new NotFoundException("Recommendation " + id + " not found"));
    }

    public Recommendation entityByShareId(String shareId) {
        return repo.findByShareId(shareId).orElseThrow(() -> new NotFoundException("Shared result " + shareId + " not found"));
    }

    @Transactional
    public void saveExplanation(Long id, String text, String language, boolean aiUsed) {
        Recommendation rec = entity(id);
        rec.setExplanation(text);
        rec.setExplanationLanguage(language);
        rec.setExplanationAiUsed(aiUsed);
        repo.save(rec);
    }

    @Transactional(readOnly = true)
    public PageDto<RecommendationSummary> history(int page, int size) {
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        Page<Recommendation> p = repo.findAllByOrderByCreatedAtDesc(PageRequest.of(Math.max(page, 0), safeSize));
        List<RecommendationSummary> content = p.getContent().stream()
                .map(r -> new RecommendationSummary(r.getId(), r.getShareId(), r.getCommodityName(), r.getTopOptionName(),
                        r.getEstimatedShelfLifeDays(), r.getCreatedAt()))
                .toList();
        return new PageDto<>(content, p.getNumber(), p.getSize(), p.getTotalElements(), p.getTotalPages());
    }

    public RecommendResponse read(Recommendation rec) {
        try {
            return json.readValue(rec.getResponseJson(), RecommendResponse.class);
        } catch (JsonProcessingException | IllegalArgumentException e) {
            throw new IllegalStateException("Stored result " + rec.getId() + " could not be read", e);
        }
    }

    private String write(Object o) {
        try {
            return json.writeValueAsString(o);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not serialise", e);
        }
    }

    // ------------------------------------------------------------------ Laminate Builder

    /** Evaluates custom layers (and optionally tests them against a food). */
    public LaminateEvaluateResponse evaluateLayers(LaminateEvaluateRequest req) {
        Catalog cat = catalog.catalog();
        List<Layer> layers = req.layers().stream()
                .map(l -> new Layer(cat.material(l.material())
                        .orElseThrow(() -> new IllegalArgumentException("Unknown material '" + l.material() + "'")),
                        l.thicknessUm()))
                .toList();
        double weight = req.packWeightG() != null ? req.packWeightG()
                : req.test() != null && req.test().packWeightG() != null ? req.test().packWeightG() : 100;
        double area = req.packAreaM2() != null ? req.packAreaM2() : barrier.estimateArea(weight);
        LaminateEval e = laminates.evaluate(layers);
        Economics econ = economics.compute(layers, area);

        FoodTestResult test = null;
        if (req.test() != null) {
            var t = req.test();
            FoodProfile food = com.packsmart.service.CatalogMapper.toFood(catalog.commodityEntity(t.commodityId()));
            Conditions cond = new Conditions(weight, area, req.packAreaM2() == null, t.shelfLifeDays(), t.storageType(),
                    t.storageTempC(), t.relativeHumidityPct(), t.transport() != null ? t.transport() : Transport.LOCAL,
                    Priority.DEFAULT);
            Candidate c = engine.testStructure(food, cond, layers, cat);
            boolean map = food.respiring();
            test = new FoodTestResult(food.name(), c.passed(),
                    map ? null : Num.sig(requiredOtr(c)), map ? null : Num.sig(requiredWvtr(c)),
                    map ? Num.sig(c.getMapCloseness()) : null,
                    c.getShelfLife().estimatedDays(), c.getShelfLife().limitingFactor().name(),
                    List.copyOf(c.getFailReasons()), List.copyOf(c.getReasons()));
        }
        return new LaminateEvaluateResponse(Num.sig(e.otr()), Num.sig(e.wvtr()), e.totalThicknessUm(), Num.sig(area),
                Num.dp(econ.gramsPerPack(), 3), Num.dp(econ.costPer1000Inr(), 2), Num.dp(econ.co2eKgPer1000(), 3),
                e.recyclable(), e.biodegradable(), e.family(), e.transparent(), e.heatSealable(), e.strength(),
                e.minTempC(), e.maxTempC(), e.approx(), test);
    }

    private Double requiredOtr(Candidate c) {
        return c.getBarrier() != null ? c.getBarrier().requiredOtr() : null;
    }

    private Double requiredWvtr(Candidate c) {
        return c.getBarrier() != null ? c.getBarrier().requiredWvtr() : null;
    }
}
