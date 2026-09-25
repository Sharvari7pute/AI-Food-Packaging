package com.packsmart.service.engine.model;

import java.util.List;

/** Full engine output before it is mapped to the API response. */
public record EngineResult(
        FoodProfile food,
        Conditions conditions,
        Requirements requirements,
        BarrierRequirements barrier,
        List<Candidate> options,
        List<Candidate> nearMisses,
        Candidate avoid,
        MapResult map) {
}
