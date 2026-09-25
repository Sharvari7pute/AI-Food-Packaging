package com.packsmart.repository;

import java.util.Optional;

import com.packsmart.entity.Laminate;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LaminateRepository extends JpaRepository<Laminate, Long> {
    Optional<Laminate> findByNameIgnoreCase(String name);
}
