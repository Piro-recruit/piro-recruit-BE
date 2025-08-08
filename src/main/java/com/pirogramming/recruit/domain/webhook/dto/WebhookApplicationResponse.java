package com.pirogramming.recruit.domain.webhook.dto;

import java.time.LocalDateTime;
import java.util.Map;

import com.pirogramming.recruit.domain.googleform.entity.GoogleForm;
import com.pirogramming.recruit.domain.webhook.entity.WebhookApplication;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebhookApplicationResponse {

    private Long id;
    private Long googleFormId;
    private String formId;
    private String formTitle;
    private String applicantName;
    private String applicantEmail;
    private String formResponseId;
    private LocalDateTime submissionTimestamp;

    // 유연한 폼 데이터
    private Map<String, Object> formData;

    // 처리 상태
    private String status;
    private String errorMessage;

    // AI 분석 결과
    private Map<String, Object> aiAnalysis;

    // 메타데이터
    private LocalDateTime createdAt;
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
            .formData(entity.getFormData())
            .status(entity.getStatus().name())
            .errorMessage(entity.getErrorMessage())
            .aiAnalysis(entity.getAiAnalysis())
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