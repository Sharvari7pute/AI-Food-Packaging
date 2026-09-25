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

/** Seasonal climate for a city (auto-fills storage temperature and RH), from cities.csv. */
@Entity
@Table(name = "cities")
@Getter
@Setter
@NoArgsConstructor
public class City {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true)
    private String name;
    private String state;
    private Double summerTempC;
    private Double summerRhPct;
    private Double monsoonTempC;
    private Double monsoonRhPct;
    private Double winterTempC;
    private Double winterRhPct;
}
