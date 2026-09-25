package com.packsmart.repository;

import java.util.Optional;

import com.packsmart.entity.Commodity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommodityRepository extends JpaRepository<Commodity, Long> {
    Optional<Commodity> findByNameIgnoreCase(String name);
}
