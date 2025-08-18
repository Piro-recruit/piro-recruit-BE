package com.pirogramming.recruit.domain.ai_summary.repository;

import com.pirogramming.recruit.domain.ai_summary.entity.ApplicationSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ApplicationSummaryRepository extends JpaRepository<ApplicationSummary, Long> {
    boolean existsByWebhookApplicationId(Long webhookApplicationId);
    
    Optional<ApplicationSummary> findByWebhookApplicationId(Long webhookApplicationId);
    
    List<ApplicationSummary> findAllByOrderByCreatedAtDesc();
    
    // 상태별 조회
    List<ApplicationSummary> findByProcessingStatusOrderByCreatedAtAsc(ApplicationSummary.ProcessingStatus status);
    
    // 대기 중인 작업 중 가장 오래된 것들 조회 (배치 처리용) - WebhookApplication, items도 함께 fetch
    @Query("SELECT DISTINCT a FROM ApplicationSummary a " +
           "LEFT JOIN FETCH a.webhookApplication " +
           "LEFT JOIN FETCH a.items " +
           "WHERE a.processingStatus = :status ORDER BY a.createdAt ASC")
    List<ApplicationSummary> findOldestPendingTasksAll(@Param("status") ApplicationSummary.ProcessingStatus status);
    
    // 배치 처리용 최적화된 쿼리 - ID만 먼저 조회하여 페이징 적용
    @Query("SELECT a.id FROM ApplicationSummary a " +
           "WHERE a.processingStatus = :status " +
           "ORDER BY a.createdAt")
    List<Long> findPendingTaskIds(@Param("status") ApplicationSummary.ProcessingStatus status, org.springframework.data.domain.Pageable pageable);
    
    // ID 기반으로 필요한 연관 데이터와 함께 조회
    @Query("SELECT DISTINCT a FROM ApplicationSummary a " +
           "LEFT JOIN FETCH a.webhookApplication w " +
           "LEFT JOIN FETCH a.items " +
           "WHERE a.id IN :ids " +
           "ORDER BY a.createdAt")
    List<ApplicationSummary> findByIdsWithAssociations(@Param("ids") List<Long> ids);
    
    // 실패한 작업 중 재시도 가능한 것들 조회
    @Query("SELECT a FROM ApplicationSummary a WHERE a.processingStatus = 'FAILED' AND a.retryCount < 3 AND a.processingCompletedAt < :retryAfter ORDER BY a.processingCompletedAt ASC")
    List<ApplicationSummary> findRetryableFailed(@Param("retryAfter") LocalDateTime retryAfter);
    
    // 처리 중인 작업 중 타임아웃된 것들 조회 (5분 이상 처리 중)
    @Query("SELECT a FROM ApplicationSummary a WHERE a.processingStatus = 'PROCESSING' AND a.processingStartedAt < :timeoutThreshold")
    List<ApplicationSummary> findTimedOutProcessing(@Param("timeoutThreshold") LocalDateTime timeoutThreshold);
    
    // 통계 조회
    @Query("SELECT a.processingStatus, COUNT(a) FROM ApplicationSummary a GROUP BY a.processingStatus")
    List<Object[]> getProcessingStatusStats();
}
