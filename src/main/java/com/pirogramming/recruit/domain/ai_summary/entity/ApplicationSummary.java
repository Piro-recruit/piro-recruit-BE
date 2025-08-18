package com.pirogramming.recruit.domain.ai_summary.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.pirogramming.recruit.domain.webhook.entity.WebhookApplication;
import com.pirogramming.recruit.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.LinkedHashMap;
import java.util.Map;
import java.time.LocalDateTime;

@Entity
@Table(name = "application_summary")
@Getter
@Setter
@NoArgsConstructor
public class ApplicationSummary extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // WebhookApplication과 1:1 관계
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "webhook_application_id", nullable = false, unique = true)
    @JsonIgnore  // JSON 직렬화 시 순환참조 방지
    private WebhookApplication webhookApplication;

    // AI 처리 상태
    @Enumerated(EnumType.STRING)
    @Column(name = "processing_status", nullable = false)
    private ProcessingStatus processingStatus = ProcessingStatus.PENDING;
    
    // AI 처리 시작 시간
    @Column(name = "processing_started_at")
    private LocalDateTime processingStartedAt;
    
    // AI 처리 완료 시간
    @Column(name = "processing_completed_at")
    private LocalDateTime processingCompletedAt;
    
    // 에러 메시지 (실패 시)
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
    
    // 재시도 횟수
    @Column(name = "retry_count", nullable = false)
    private Integer retryCount = 0;

    // 요약 결과를 Key-Value로 저장
    @ElementCollection
    @CollectionTable(name = "application_summary_items", joinColumns = @JoinColumn(name = "summary_id"))
    @MapKeyColumn(name = "item_key", length = 100)
    @Column(name = "item_value", columnDefinition = "TEXT")
    private Map<String, String> items = new LinkedHashMap<>();
    
    // AI 처리 상태 열거형
    public enum ProcessingStatus {
        PENDING,     // 대기 중
        PROCESSING,  // 처리 중
        COMPLETED,   // 완료
        FAILED       // 실패
    }

    @Builder
    public ApplicationSummary(WebhookApplication webhookApplication,
                              Map<String, String> items,
                              ProcessingStatus processingStatus,
                              LocalDateTime processingStartedAt,
                              LocalDateTime processingCompletedAt,
                              String errorMessage,
                              Integer retryCount) {
        this.webhookApplication = webhookApplication;
        if (items != null) this.items.putAll(items);
        this.processingStatus = processingStatus != null ? processingStatus : ProcessingStatus.PENDING;
        this.processingStartedAt = processingStartedAt;
        this.processingCompletedAt = processingCompletedAt;
        this.errorMessage = errorMessage;
        this.retryCount = retryCount != null ? retryCount : 0;
    }
    
    // 상태 업데이트 메서드들
    public void markAsProcessing() {
        this.processingStatus = ProcessingStatus.PROCESSING;
        this.processingStartedAt = LocalDateTime.now();
    }
    
    public void markAsCompleted() {
        this.processingStatus = ProcessingStatus.COMPLETED;
        this.processingCompletedAt = LocalDateTime.now();
        this.errorMessage = null;
    }
    
    public void markAsFailed(String errorMessage) {
        this.processingStatus = ProcessingStatus.FAILED;
        this.processingCompletedAt = LocalDateTime.now();
        this.errorMessage = errorMessage;
        this.retryCount++;
    }
    
    public boolean canRetry() {
        return this.retryCount < 3 && this.processingStatus == ProcessingStatus.FAILED;
    }
}
