package com.pirogramming.recruit.domain.webhook.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.pirogramming.recruit.domain.ai_summary.entity.ApplicationSummary;
import com.pirogramming.recruit.domain.evaluation.entity.Evaluation;
import com.pirogramming.recruit.domain.googleform.entity.GoogleForm;
import com.pirogramming.recruit.global.entity.BaseTimeEntity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "webhook_applications",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"google_form_id", "applicant_email"}), @UniqueConstraint(columnNames = {"homepage_user_id"})
    })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WebhookApplication extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 리크루팅과 연관관계
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "google_form_id", nullable = false)
    @JsonIgnore  // JSON 직렬화 시 순환참조 방지
    private GoogleForm googleForm; // 몇 기 리쿠르팅인지

    // 고정 필드들 (필수 정보)
    @Column(nullable = false)
    private String applicantName; // 지원자 이름

    @Column(nullable = false)
    private String applicantEmail; // 지원자 이메일(중복 방지용)

    // 추가된 개별 필드들
    @Column
    private String school; // 학교

    @Column
    private String department; // 학과

    @Column
    private String grade; // 학년

    @Column
    private String major; // 전공

    @Column
    private String phoneNumber; // 전화번호

    @Column(nullable = false, unique = true)
    private String formResponseId; // 구글 폼 응답 고유 ID

    @Column(nullable = false)
    private LocalDateTime submissionTimestamp; // 구글 폼 제출 시간

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

    // 홈페이지 연동을 위한 필드들
    @Column(name = "homepage_user_id", unique = true)
    private Long homepageUserId; // 홈페이지에서 사용할 순차적 ID (1, 2, 3...)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PassStatus passStatus = PassStatus.PENDING; // 합격 상태

    // 평가 관련 필드들
    @Column(name = "average_score")
    private Double averageScore; // 평균 점수

    @Column(name = "evaluation_count", nullable = false)
    private Integer evaluationCount = 0; // 평가 개수

    // 연관된 평가들 (지원서 삭제 시 함께 삭제)
    @OneToMany(mappedBy = "application", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore  // JSON 직렬화 시 순환참조 방지
    private List<Evaluation> evaluations = new ArrayList<>();

    // AI 요약 (1:1 관계, 지원서 삭제 시 함께 삭제)
    @OneToOne(mappedBy = "webhookApplication", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private ApplicationSummary applicationSummary;

    @Builder
    public WebhookApplication(GoogleForm googleForm, String applicantName, String applicantEmail,
                              String formResponseId, LocalDateTime submissionTimestamp, Map<String, Object> formData,
                              String school, String department, String grade, String major, String phoneNumber) {
        this.googleForm = googleForm;
        this.applicantName = applicantName;
        this.applicantEmail = applicantEmail;
        this.formResponseId = formResponseId;
        this.submissionTimestamp = submissionTimestamp;
        this.formData = formData;
        this.school = school;
        this.department = department;
        this.grade = grade;
        this.major = major;
        this.phoneNumber = phoneNumber;
        this.status = ProcessingStatus.PENDING;
        this.passStatus = PassStatus.PENDING;
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

    // 호메이지 User ID 설정
    public void setHomepageUserId(Long homepageUserId) {
        this.homepageUserId = homepageUserId;
    }

    // 합격 상태 관리 메서드들
    public void markAsFirstPass() {
        this.passStatus = PassStatus.FIRST_PASS;
    }

    public void markAsFinalPass() {
        this.passStatus = PassStatus.FINAL_PASS;
    }

    public void markAsPassFailed() {
        this.passStatus = PassStatus.FAILED;
    }

    public void resetPassStatus() {
        this.passStatus = PassStatus.PENDING;
    }

    // 평가 점수 업데이트 메서드들
    public void updateEvaluationStatistics(Double newAverageScore, Integer newEvaluationCount) {
        this.averageScore = newAverageScore;
        this.evaluationCount = newEvaluationCount;
    }

    public void incrementEvaluationCount() {
        this.evaluationCount = this.evaluationCount + 1;
    }

    public void decrementEvaluationCount() {
        if (this.evaluationCount > 0) {
            this.evaluationCount = this.evaluationCount - 1;
        }
    }

    // 웹훅 처리 상태 enum
    public enum ProcessingStatus {
        PENDING,    // 처리 대기(웹훅으로 받았지만 아직 처리 안됨)
        COMPLETED,  // 처리 완료
        FAILED      // 처리 실패
    }

    // 합격 상태 enum
    public enum PassStatus {
        PENDING(0),     // 대기중/미정
        FAILED(0),      // 불합격
        FIRST_PASS(1),  // 1차 합격
        FINAL_PASS(2);  // 최종 합격

        private final int csvValue;

        PassStatus(int csvValue) {
            this.csvValue = csvValue;
        }

        public int getCsvValue() {
            return csvValue;
        }
    }
}