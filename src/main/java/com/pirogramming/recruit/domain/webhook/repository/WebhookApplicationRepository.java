package com.pirogramming.recruit.domain.webhook.repository;

import com.pirogramming.recruit.domain.webhook.entity.WebhookApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WebhookApplicationRepository extends JpaRepository<WebhookApplication, Long> {

    // 이메일로 지원서 조회
    Optional<WebhookApplication> findByEmail(String email);

    // 구글 폼 응답 ID로 중복 체크
    boolean existsByFormResponseId(String formResponseId);

    // 처리 상태별 조회
    List<WebhookApplication> findByStatus(WebhookApplication.ProcessingStatus status);

    // 처리 대기 중인 지원서 개수
    @Query("SELECT COUNT(w) FROM WebhookApplication w WHERE w.status = 'PENDING'")
    long countPendingApplications();

    // 최근 제출된 지원서 조회 (제출시간 기준 내림차순)
    @Query("SELECT w FROM WebhookApplication w ORDER BY w.submissionTimestamp DESC")
    List<WebhookApplication> findAllOrderBySubmissionTimeDesc();

    // 이름과 이메일로 지원서 존재 확인
    boolean existsByNameAndEmail(String name, String email);
}