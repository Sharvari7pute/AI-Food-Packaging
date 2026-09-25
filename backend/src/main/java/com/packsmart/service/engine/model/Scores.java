package com.packsmart.service.engine.model;

/** Step F scores, each 0-1, the weighted total and (after ranking) the TOPSIS closeness coefficient. */
public record Scores(double barrier, double cost, double eco, double strength, double total, Double topsis) {

    public Scores(double barrier, double cost, double eco, double strength, double total) {
        this(barrier, cost, eco, strength, total, null);
    }

    public Scores withTopsis(double closeness) {
        return new Scores(barrier, cost, eco, strength, total, closeness);
    }
}
