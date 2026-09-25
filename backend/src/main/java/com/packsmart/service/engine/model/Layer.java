package com.packsmart.service.engine.model;

/** One layer of a pack structure: a material at a thickness (µm). */
public record Layer(MaterialData material, double thicknessUm) {
}
