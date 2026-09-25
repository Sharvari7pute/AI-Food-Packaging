package com.packsmart.service.engine.model;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

/** A single film or laminate being evaluated by the engine. */
@Getter
@Setter
public class Candidate {
    private final String name;
    private final CandidateKind kind;
    private final List<Layer> layers;
    private final LaminateEval eval;
    private final Economics economics;
    private final List<String> failReasons = new ArrayList<>();
    private final List<String> reasons = new ArrayList<>();
    private ShelfLife shelfLife;
    private Scores scores;
    private boolean perforationNeeded;
    /** MAP only: film OTR / required OTR (closer to 1 is better). */
    private Double mapCloseness;
    /** Requirements this candidate was tested against (Laminate Builder food test). */
    private BarrierRequirements barrier;

    public Candidate(String name, CandidateKind kind, List<Layer> layers, LaminateEval eval, Economics economics) {
        this.name = name;
        this.kind = kind;
        this.layers = layers;
        this.eval = eval;
        this.economics = economics;
    }

    public boolean passed() {
        return failReasons.isEmpty();
    }
}
