package com.packsmart.service.engine.model;

/** Target modified-atmosphere gas ranges (% v/v) and storage temperature for a produce item. */
public record MapTargetData(String name, double o2Min, double o2Max, double co2Min, double co2Max, Double storageTempC) {
}
