package com.pirogramming.recruit.domain.webhook.dto;

import java.time.LocalDateTime;
import java.util.Map;

import com.pirogramming.recruit.domain.googleform.entity.GoogleForm;
import com.pirogramming.recruit.domain.webhook.entity.WebhookApplication;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "지원서 응답 DTO")
public class WebhookApplicationResponse {

    @Schema(description = "지원서 ID", example = "1")
    private Long id;
    
    @Schema(description = "구글 폼 ID", example = "1")
    private Long googleFormId;
    
    @Schema(description = "구글 폼 식별자", example = "1FAIpQLSe...")
    private String formId;
    
    @Schema(description = "구글 폼 제목", example = "25기 리크루팅")
    private String formTitle;
    
    @Schema(description = "지원자 이름", example = "홍길동")
    private String applicantName;
    
    @Schema(description = "지원자 이메일", example = "hong@example.com")
    private String applicantEmail;
    
    @Schema(description = "구글 폼 응답 ID", example = "2_ABaOnud...")
    private String formResponseId;
    
    @Schema(description = "지원서 제출 시간", example = "2024-01-01T10:00:00")
    private LocalDateTime submissionTimestamp;

    @Schema(description = "학교", example = "서울대학교")
    private String school;
    
    @Schema(description = "학과", example = "컴퓨터공학과")
    private String department;
    
    @Schema(description = "학년", example = "3학년")
    private String grade;
    
    @Schema(description = "전공", example = "컴퓨터공학")
    private String major;
    
    @Schema(description = "전화번호", example = "010-1234-5678")
    private String phoneNumber;

    @Schema(description = "구글 폼 응답 데이터")
    private Map<String, Object> formData;

    @Schema(description = "처리 상태", example = "COMPLETED", allowableValues = {"PENDING", "COMPLETED", "FAILED"})
    private String status;
    
    @Schema(description = "오류 메시지", example = "")
    private String errorMessage;

    @Schema(description = "AI 분석 결과")
    private Map<String, Object> aiAnalysis;

    @Schema(description = "평가 평균 점수", example = "82.5")
    private Double averageScore;
    
    @Schema(description = "평가 개수", example = "3")
    private Integer evaluationCount;

    @Schema(description = "합격 상태", example = "PENDING", allowableValues = {"PENDING", "FAILED", "FIRST_PASS", "FINAL_PASS"})
    private String passStatus;

    @Schema(description = "지원서 생성 시간", example = "2024-01-01T10:00:00")
    private LocalDateTime createdAt;
    
    @Schema(description = "지원서 수정 시간", example = "2024-01-01T10:30:00")
    private LocalDateTime updatedAt;

    // Entity를 DTO로 변환
    public static WebhookApplicationResponse from(WebhookApplication entity) {
        GoogleForm googleForm = entity.getGoogleForm();
        return WebhookApplicationResponse.builder()
            .id(entity.getId())
            .googleFormId(googleForm != null ? googleForm.getId() : null)
            .formId(googleForm != null ? googleForm.getFormId() : null)
            .formTitle(googleForm != null ? googleForm.getTitle() : null)
            .applicantName(entity.getApplicantName())
            .applicantEmail(entity.getApplicantEmail())
            .formResponseId(entity.getFormResponseId())
            .submissionTimestamp(entity.getSubmissionTimestamp())
            .school(entity.getSchool())
            .department(entity.getDepartment())
            .grade(entity.getGrade())
            .major(entity.getMajor())
            .phoneNumber(entity.getPhoneNumber())
            .formData(entity.getFormData())
            .status(entity.getStatus().name())
            .errorMessage(entity.getErrorMessage())
            .aiAnalysis(entity.getAiAnalysis())
            .averageScore(entity.getAverageScore())
            .evaluationCount(entity.getEvaluationCount())
            .passStatus(entity.getPassStatus().name())
            .createdAt(entity.getCreatedAt())
            .updatedAt(entity.getUpdatedAt())
            .build();
    }

    // 특정 폼 데이터 값 조회 편의 메서드
    public Object getFormValue(String key) {
        return formData != null ? formData.get(key) : null;
    }

    // 특정 폼 데이터를 문자열로 조회
    public String getFormValueAsString(String key) {
        Object value = getFormValue(key);
        return value != null ? value.toString() : null;
    }

    // AI 분석 결과 조회 편의 메서드
    public Object getAiAnalysisValue(String key) {
        return aiAnalysis != null ? aiAnalysis.get(key) : null;
    }
}