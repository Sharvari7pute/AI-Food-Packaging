package com.packsmart.repository;

import java.util.Optional;

import com.packsmart.entity.MapTarget;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MapTargetRepository extends JpaRepository<MapTarget, Long> {
    Optional<MapTarget> findByNameIgnoreCase(String name);
}
