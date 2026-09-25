package com.packsmart.service.engine.model;

/** Step F scores, each 0-1, plus the weighted total. */
public record Scores(double barrier, double cost, double eco, double strength, double total) {
}
