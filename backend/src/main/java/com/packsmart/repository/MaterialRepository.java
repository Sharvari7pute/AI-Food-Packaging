package com.packsmart.repository;

import java.util.Optional;

import com.packsmart.entity.Material;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MaterialRepository extends JpaRepository<Material, Long> {
    Optional<Material> findByNameIgnoreCase(String name);
}
