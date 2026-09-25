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

/** A multi-layer structure. {@code layers} = "Material:thickness_um;..." outside to inside. */
@Entity
@Table(name = "laminates")
@Getter
@Setter
@NoArgsConstructor
public class Laminate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true)
    private String name;
    @Column(nullable = false, length = 1000)
    private String layers;
    @Column(length = 1000)
    private String typicalUse;
}
