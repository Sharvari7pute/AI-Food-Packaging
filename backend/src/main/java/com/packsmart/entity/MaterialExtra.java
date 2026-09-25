package com.packsmart.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Extra per-material data (polymer family, CO2e factor) from material_extras.csv. */
@Entity
@Table(name = "material_extras")
@Getter
@Setter
@NoArgsConstructor
public class MaterialExtra {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true)
    private String name;
    private String family;
    private Double co2eKgPerKg;
    @Column(length = 1000)
    private String notes;
}
