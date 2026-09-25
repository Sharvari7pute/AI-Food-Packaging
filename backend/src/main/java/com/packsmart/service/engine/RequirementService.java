package com.packsmart.service.engine;

import java.util.ArrayList;
import java.util.List;

import com.packsmart.config.EngineProperties;
import com.packsmart.service.engine.model.Conditions;
import com.packsmart.service.engine.model.FoodProfile;
import com.packsmart.service.engine.model.MoistureMode;
import com.packsmart.service.engine.model.O2Barrier;
import com.packsmart.service.engine.model.Requirements;
import com.packsmart.service.engine.model.StorageType;
import com.packsmart.service.engine.model.Transport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Step A - turns food properties and storage conditions into pack requirements. Pure. */
@Service
@RequiredArgsConstructor
public class RequirementService {

    private static final String HIGH = "high";
    private static final String MEDIUM = "medium";

    private final EngineProperties props;

    /**
     * Derives requirements.
     * <ul>
     *   <li>o2Barrier = HIGH if fat &gt; fatThreshold (only for foods with aw ≤ keepInAwThreshold, see DECISIONS.md)
     *       or o2_sensitive = high; MEDIUM if o2_sensitive = medium; else LOW.</li>
     *   <li>moistureMode = BREATHABLE if respiring; KEEP_OUT if aw &lt; keepOutAw; KEEP_IN if aw &gt; keepInAw; else MODERATE.</li>
     *   <li>opaque = light_sensitive = high; frozen = storage FROZEN; needsMap = respiring;
     *       needsStrength = transport LONG_DISTANCE.</li>
     * </ul>
     */
    public Requirements derive(FoodProfile food, Conditions cond) {
        List<String> reasons = new ArrayList<>();
        double aw = food.waterActivity();
        double fat = food.fatPct();

        O2Barrier o2 = O2Barrier.LOW;
        boolean fatRuleApplies = aw <= props.getKeepInAwThreshold();
        if (fat > props.getFatThresholdPct() && fatRuleApplies) {
            o2 = O2Barrier.HIGH;
            reasons.add("Fat " + Num.fmt(fat) + "% > " + Num.fmt(props.getFatThresholdPct())
                    + "% → oxygen barrier HIGH (fats turn rancid with oxygen)");
        }
        if (HIGH.equals(food.o2Sensitive())) {
            o2 = O2Barrier.HIGH;
            reasons.add("Highly oxygen-sensitive food → oxygen barrier HIGH");
        }
        if (o2 != O2Barrier.HIGH) {
            if (fat > props.getFatThresholdPct()) {
                reasons.add("Fat " + Num.fmt(fat) + "% but aw " + Num.fmt(aw) + " > " + Num.fmt(props.getKeepInAwThreshold())
                        + " → moist food spoils by microbes before fat goes rancid, so the fat rule is not applied");
            }
            if (MEDIUM.equals(food.o2Sensitive())) {
                o2 = O2Barrier.MEDIUM;
                reasons.add("Medium oxygen sensitivity → oxygen barrier MEDIUM");
            } else if (!food.respiring()) {
                reasons.add("Low oxygen sensitivity and fat ≤ " + Num.fmt(props.getFatThresholdPct())
                        + "% → oxygen barrier LOW (no OTR limit)");
            }
        }

        MoistureMode moisture;
        if (food.respiring()) {
            moisture = MoistureMode.BREATHABLE;
            reasons.add("Fresh produce keeps breathing after harvest → breathable film with modified atmosphere (MAP)");
        } else if (aw < props.getKeepOutAwThreshold()) {
            moisture = MoistureMode.KEEP_OUT;
            reasons.add("aw " + Num.fmt(aw) + " < " + Num.fmt(props.getKeepOutAwThreshold())
                    + " → keep moisture OUT (dry food goes soggy)");
        } else if (aw > props.getKeepInAwThreshold()) {
            moisture = MoistureMode.KEEP_IN;
            reasons.add("aw " + Num.fmt(aw) + " > " + Num.fmt(props.getKeepInAwThreshold())
                    + " → keep moisture IN (moist food dries out)");
        } else {
            moisture = MoistureMode.MODERATE;
            reasons.add("aw " + Num.fmt(aw) + " is between " + Num.fmt(props.getKeepOutAwThreshold()) + " and "
                    + Num.fmt(props.getKeepInAwThreshold()) + " → moderate moisture barrier, no strict WVTR limit");
        }

        boolean opaque = HIGH.equals(food.lightSensitive());
        if (opaque) {
            reasons.add("Light-sensitive → opaque pack required");
        }
        boolean frozen = cond.storageType() == StorageType.FROZEN;
        if (frozen) {
            reasons.add("Frozen storage → materials must stay flexible down to " + Num.fmt(props.getFrozenMinTempC()) + " °C");
        }
        boolean needsMap = food.respiring();
        boolean needsStrength = cond.transport() == Transport.LONG_DISTANCE;
        if (needsStrength) {
            reasons.add("Long-distance transport → strength ≥ " + props.getMinStrengthLongDistance() + "/"
                    + props.getStrengthScale() + " required");
        }
        return new Requirements(o2, moisture, opaque, frozen, needsMap, needsStrength, List.copyOf(reasons));
    }
}
