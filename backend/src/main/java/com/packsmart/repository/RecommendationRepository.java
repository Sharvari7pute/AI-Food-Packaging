package com.packsmart.repository;

import java.util.Optional;

import com.packsmart.entity.Recommendation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecommendationRepository extends JpaRepository<Recommendation, Long> {
    Optional<Recommendation> findByShareId(String shareId);

    Page<Recommendation> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
