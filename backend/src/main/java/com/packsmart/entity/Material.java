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

/** A packaging material from materials.csv (REAL team data). OTR/WVTR are normalised to 25 µm. */
@Entity
@Table(name = "materials")
@Getter
@Setter
@NoArgsConstructor
public class Material {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true)
    private String name;
    private String type;
    private Double otr25um;
    private Double wvtr25um;
    private Double costPerKgInr;
    private Double densityGCm3;
    private Double minTempC;
    private Double maxTempC;
    private Boolean recyclable;
    private Boolean biodegradable;
    private Boolean transparent;
    private Boolean heatSealable;
    private Integer strength1to5;
    @Column(length = 1000)
    private String sourceUrl;
    @Column(length = 2000)
    private String notes;
}
