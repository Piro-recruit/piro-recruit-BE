package com.pirogramming.recruit.domain.ai_summary.repository;

import com.pirogramming.recruit.domain.ai_summary.entity.ApplicationSummary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ApplicationSummaryRepository extends JpaRepository<ApplicationSummary, Long> {
    boolean existsByWebhookApplicationId(Long webhookApplicationId);
    
    Optional<ApplicationSummary> findByWebhookApplicationId(Long webhookApplicationId);
    
    List<ApplicationSummary> findAllByOrderByCreatedAtDesc();
}
