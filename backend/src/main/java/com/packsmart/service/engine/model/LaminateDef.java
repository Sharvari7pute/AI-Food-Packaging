package com.packsmart.service.engine.model;

import java.util.List;

/** A predefined laminate structure (outside to inside). */
public record LaminateDef(String name, List<Layer> layers, String typicalUse) {
}
