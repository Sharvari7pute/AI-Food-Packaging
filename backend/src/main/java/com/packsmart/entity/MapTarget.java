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

/** Modified-atmosphere targets for respiring produce, from map_targets.csv. */
@Entity
@Table(name = "map_targets")
@Getter
@Setter
@NoArgsConstructor
public class MapTarget {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true)
    private String name;
    private Double targetO2Min;
    private Double targetO2Max;
    private Double targetCo2Min;
    private Double targetCo2Max;
    private Double storageTempC;
    @Column(length = 1000)
    private String sourceUrl;
}
