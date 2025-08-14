package com.pirogramming.recruit.domain.ai_summary.repository;

import com.pirogramming.recruit.domain.ai_summary.entity.ApplicationSummary;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApplicationSummaryRepository extends JpaRepository<ApplicationSummary, Long> {
    boolean existsByFormResponseIdAndApplicantEmail(String formResponseId, String applicantEmail);
}
