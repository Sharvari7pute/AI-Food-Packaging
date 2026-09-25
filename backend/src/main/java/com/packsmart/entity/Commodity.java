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

/** A food product and its properties from commodities.csv. */
@Entity
@Table(name = "commodities")
@Getter
@Setter
@NoArgsConstructor
public class Commodity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true)
    private String name;
    private String nameHi;
    private String category;
    private Double moisturePct;
    private Double waterActivity;
    private Double criticalAw;
    private Double fatPct;
    private String o2Sensitive;
    private String lightSensitive;
    private Boolean respiring;
    private Double respirationRate;
    private Integer defaultShelfLifeDays;
    private Double ph;
    private Double storageTempMinC;
    private Double storageTempMaxC;
    private String mainDeteriorationFactor;
    @Column(columnDefinition = "text")
    private String sourceUrl;
    @Column(columnDefinition = "text")
    private String notes;
}
