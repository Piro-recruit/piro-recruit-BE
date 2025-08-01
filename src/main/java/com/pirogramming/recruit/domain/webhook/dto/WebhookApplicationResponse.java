package com.pirogramming.recruit.domain.webhook.dto;

import com.pirogramming.recruit.domain.webhook.entity.WebhookApplication;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebhookApplicationResponse {

    private Long id;
    private String name;
    private String email;
    private String phone;
    private String school;
    private String major;
    private String portfolioUrl;
    private String introduction;
    private String motivation;
    private String formResponseId;
    private String submissionTimestamp;
    private String status;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Entity를 DTO로 변환
    public static WebhookApplicationResponse from(WebhookApplication entity) {
        return WebhookApplicationResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .email(entity.getEmail())
                .phone(entity.getPhone())
                .school(entity.getSchool())
                .major(entity.getMajor())
                .portfolioUrl(entity.getPortfolioUrl())
                .introduction(entity.getIntroduction())
                .motivation(entity.getMotivation())
                .formResponseId(entity.getFormResponseId())
                .submissionTimestamp(entity.getSubmissionTimestamp())
                .status(entity.getStatus().name())
                .errorMessage(entity.getErrorMessage())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}