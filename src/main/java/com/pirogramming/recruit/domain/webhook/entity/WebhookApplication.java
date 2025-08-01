package com.pirogramming.recruit.domain.webhook.entity;

import com.pirogramming.recruit.domain.recruitment.entity.Recruitment;
import com.pirogramming.recruit.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.util.Map;

@Entity
@Table(name = "webhook_applications")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WebhookApplication extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 🔗 리크루팅과 연관관계
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recruitment_id", nullable = false)
    private Recruitment recruitment; // 몇 기 리쿠르팅인지

    // 고정 필드들 (필수 정보)
    @Column(nullable = false)
    private String applicantName; // 지원자 이름

    @Column(nullable = false, unique = true)
    private String applicantEmail; // 지원자 이메일(중복 방지용)

    @Column(nullable = false)
    private String formResponseId; // 구글 폼 응답 고유 ID

    @Column(nullable = false)
    private String submissionTimestamp; // 구글 폼 제출 시간

    // 유연한 필드 (JSON으로 저장)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "form_data", columnDefinition = "jsonb")
    private Map<String, Object> formData; // 구글 폼의 모든 응답 데이터

    // 처리 상태 관리
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProcessingStatus status = ProcessingStatus.PENDING;

    @Column(columnDefinition = "TEXT")
    private String errorMessage; // 처리 실패 시 오류 메시지

    // AI 분석 결과 (나중에 AI로 지원서 요약/채점 결과 저장 기능)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "ai_analysis", columnDefinition = "jsonb")
    private Map<String, Object> aiAnalysis;

    @Builder
    public WebhookApplication(Recruitment recruitment,
                              String applicantName, String applicantEmail, String formResponseId,
                              String submissionTimestamp, Map<String, Object> formData) {
        this.recruitment = recruitment;
        this.applicantName = applicantName;
        this.applicantEmail = applicantEmail;
        this.formResponseId = formResponseId;
        this.submissionTimestamp = submissionTimestamp;
        this.formData = formData;
        this.status = ProcessingStatus.PENDING;
    }

    // 처리 완료 상태로 변경
    public void markAsProcessed() {
        this.status = ProcessingStatus.COMPLETED;
    }

    // 처리 실패 상태로 변경
    public void markAsFailed(String errorMessage) {
        this.status = ProcessingStatus.FAILED;
        this.errorMessage = errorMessage;
    }

    // AI 분석 결과 저장
    public void updateAiAnalysis(Map<String, Object> aiAnalysis) {
        this.aiAnalysis = aiAnalysis;
    }

    // 특정 폼 데이터 조회
    public Object getFormDataValue(String key) {
        return formData != null ? formData.get(key) : null;
    }

    // 특정 폼 데이터 조회 (문자열로 변환)
    public String getFormDataAsString(String key) {
        Object value = getFormDataValue(key);
        return value != null ? value.toString() : null;
    }

    public enum ProcessingStatus {
        PENDING,    // 처리 대기(웹훅으로 받았지만 아직 처리 안됨)
        COMPLETED,  // 처리 완료
        FAILED      // 처리 실패
    }
}