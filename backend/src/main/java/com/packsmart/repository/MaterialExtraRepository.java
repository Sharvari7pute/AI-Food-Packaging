package com.packsmart.repository;

import java.util.Optional;

import com.packsmart.entity.MaterialExtra;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MaterialExtraRepository extends JpaRepository<MaterialExtra, Long> {
    Optional<MaterialExtra> findByNameIgnoreCase(String name);
}
