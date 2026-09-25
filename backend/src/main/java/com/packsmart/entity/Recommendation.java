package com.packsmart.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** A saved engine run. Never touched by the reseed. */
@Entity
@Table(name = "recommendations")
@Getter
@Setter
@NoArgsConstructor
public class Recommendation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 36)
    private String shareId;
    private String commodityName;
    private String topOptionName;
    private Integer estimatedShelfLifeDays;
    @Column(columnDefinition = "text")
    private String requestJson;
    @Column(columnDefinition = "text")
    private String responseJson;
    @Column(columnDefinition = "text")
    private String explanation;
    private String explanationLanguage;
    private Boolean explanationAiUsed;
    @Column(nullable = false)
    private Instant createdAt;
}
