package com.pirogramming.recruit.domain.ai_summary.repository;

import com.pirogramming.recruit.domain.ai_summary.entity.ApplicationSummary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ApplicationSummaryRepository extends JpaRepository<ApplicationSummary, Long> {
    boolean existsByFormResponseIdAndApplicantEmail(String formResponseId, String applicantEmail);

    // 추가: 지원서-요약 매칭(점수 가져오기)
    Optional<ApplicationSummary> findByFormResponseIdAndApplicantEmail(String formResponseId, String applicantEmail);
}
