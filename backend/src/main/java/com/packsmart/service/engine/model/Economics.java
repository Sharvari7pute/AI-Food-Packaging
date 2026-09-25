package com.packsmart.service.engine.model;

/** Step E output: material use, cost and carbon footprint. */
public record Economics(double gramsPerPack, double costPerPackInr, double costPer1000Inr, double co2eKgPer1000) {
}
