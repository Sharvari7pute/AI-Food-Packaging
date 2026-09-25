package com.packsmart.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.packsmart.config.AppProperties;
import com.packsmart.dto.RecommendResponse;
import com.packsmart.dto.RecommendResponse.AvoidDto;
import com.packsmart.dto.RecommendResponse.CurvePointDto;
import com.packsmart.dto.RecommendResponse.GasMixDto;
import com.packsmart.dto.RecommendResponse.InputsEcho;
import com.packsmart.dto.RecommendResponse.LayerDto;
import com.packsmart.dto.RecommendResponse.MapDto;
import com.packsmart.dto.RecommendResponse.NearMissDto;
import com.packsmart.dto.RecommendResponse.OptionDto;
import com.packsmart.dto.RecommendResponse.RequirementsDto;
import com.packsmart.dto.RecommendResponse.ScoresDto;
import com.packsmart.service.engine.Num;
import com.packsmart.service.engine.model.Candidate;
import com.packsmart.service.engine.model.Conditions;
import com.packsmart.service.engine.model.EngineResult;
import com.packsmart.service.engine.model.FoodProfile;
import com.packsmart.service.engine.model.Layer;
import com.packsmart.service.engine.model.MapResult;
import com.packsmart.service.engine.model.MapTargetData;
import com.packsmart.service.engine.model.Requirements;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Engine result → API response (rounding happens here, never inside the engine). */
@Component
@RequiredArgsConstructor
public class ResponseMapper {

    private static final double PERCENT = 100.0;

    private final AppProperties app;

    public RecommendResponse toResponse(EngineResult r, String language, Long id, String shareId, Instant createdAt) {
        FoodProfile f = r.food();
        Conditions c = r.conditions();
        Requirements req = r.requirements();
        InputsEcho inputs = new InputsEcho(f.commodityId(), f.name(), f.moisturePct(), f.waterActivity(), f.criticalAw(),
                f.fatPct(), f.o2Sensitive(), f.lightSensitive(), f.respiring(), f.respirationRate(), c.packWeightG(),
                Num.sig(c.areaM2()), c.areaEstimated(), c.shelfLifeDays(), c.storageType().name(), c.storageTempC(),
                c.relativeHumidityPct(), c.transport().name(), c.priority().name(), language);
        List<String> reasons = new ArrayList<>(req.reasons());
        reasons.addAll(r.barrier().reasons());
        RequirementsDto reqDto = new RequirementsDto(req.o2Barrier().name(), req.moistureMode().name(), req.opaque(),
                req.frozen(), req.needsMap(), req.needsStrength(), List.copyOf(reasons));

        List<OptionDto> options = new ArrayList<>();
        for (int i = 0; i < r.options().size(); i++) {
            options.add(option(i + 1, r.options().get(i)));
        }
        List<NearMissDto> nearMisses = r.nearMisses().stream().map(this::nearMiss).toList();
        AvoidDto avoid = r.avoid() == null ? null : avoid(r.avoid());
        return new RecommendResponse(id, shareId, f.name(), f.nameHi(), f.aiEstimated(), inputs, reqDto,
                Num.sig(r.barrier().requiredOtr()), Num.sig(r.barrier().requiredWvtr()), options, nearMisses, avoid,
                map(r.map()), app.getDisclaimer(), createdAt);
    }

    private static List<LayerDto> layers(List<Layer> layers) {
        return layers.stream().map(l -> new LayerDto(l.material().name(), l.thicknessUm())).toList();
    }

    private static Double days(Double d) {
        if (d == null) {
            return null;
        }
        return Double.isInfinite(d) ? null : Num.dp(d, 1);
    }

    private OptionDto option(int rank, Candidate c) {
        var e = c.getEval();
        var s = c.getScores();
        var sl = c.getShelfLife();
        return new OptionDto(rank, c.getName(), c.getKind().name(), layers(c.getLayers()), e.totalThicknessUm(),
                Num.sig(e.otr()), Num.sig(e.wvtr()),
                new ScoresDto(Num.dp(s.barrier(), 3), Num.dp(s.cost(), 3), Num.dp(s.eco(), 3), Num.dp(s.strength(), 3),
                        Num.dp(s.total(), 3)),
                sl.estimatedDays(), sl.limitingFactor().name(), days(sl.o2Days()), days(sl.moistureDays()),
                Num.dp(c.getEconomics().costPer1000Inr(), 2), Num.dp(c.getEconomics().co2eKgPer1000(), 3),
                Num.dp(c.getEconomics().gramsPerPack(), 3), e.recyclable(), e.biodegradable(), e.family(), e.transparent(),
                e.heatSealable(), e.strength(), e.minTempC(), e.maxTempC(), e.approx(), c.isPerforationNeeded(),
                List.copyOf(c.getReasons()),
                sl.curve().stream().map(p -> new CurvePointDto(p.day(), p.o2UsedPct(), p.moistureUsedPct())).toList());
    }

    private NearMissDto nearMiss(Candidate c) {
        return new NearMissDto(c.getName(), c.getKind().name(), layers(c.getLayers()), Num.sig(c.getEval().otr()),
                Num.sig(c.getEval().wvtr()), c.getShelfLife().estimatedDays(), c.getShelfLife().limitingFactor().name(),
                Num.dp(c.getEconomics().costPer1000Inr(), 2), List.copyOf(c.getFailReasons()));
    }

    private AvoidDto avoid(Candidate c) {
        return new AvoidDto(c.getName(), c.getKind().name(), c.getEval().totalThicknessUm(), Num.sig(c.getEval().otr()),
                Num.sig(c.getEval().wvtr()), String.join("; ", c.getFailReasons()));
    }

    private MapDto map(MapResult m) {
        if (m == null) {
            return null;
        }
        MapTargetData t = m.target();
        double o2 = m.targetO2Pct();
        double co2 = t != null ? (t.co2Min() + t.co2Max()) / 2.0 : 0;
        GasMixDto mix = new GasMixDto(Num.dp(o2, 1), Num.dp(co2, 1), Num.dp(PERCENT - o2 - co2, 1));
        return new MapDto(t != null, t != null ? t.o2Min() : null, t != null ? t.o2Max() : null,
                t != null ? t.co2Min() : null, t != null ? t.co2Max() : null, t != null ? t.storageTempC() : null, mix,
                Num.sig(m.respirationMlPerDay()), Num.sig(m.requiredOtr()), m.filmMaterial(), m.filmThicknessUm(),
                Num.sig(m.filmOtr()), m.perforationNeeded(), m.reasons(), m.note());
    }
}
