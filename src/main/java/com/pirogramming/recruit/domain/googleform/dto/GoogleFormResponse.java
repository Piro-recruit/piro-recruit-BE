package com.pirogramming.recruit.domain.googleform.dto;

import java.time.LocalDateTime;

import com.pirogramming.recruit.domain.googleform.entity.GoogleForm;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class GoogleFormResponse {

    private Long id;
    private String formId;
    private String title;
    private String formUrl;
    private String sheetUrl;
    private Boolean isActive;
    private String description;
    private LocalDateTime recruitingStartDate;
    private LocalDateTime recruitingEndDate;

    // 추가 정보
    private Long applicationCount; // 지원서 개수

    // 메타데이터
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Entity를 DTO로 변환
    public static GoogleFormResponse from(GoogleForm entity) {
        return GoogleFormResponse.builder()
                .id(entity.getId())
                .formId(entity.getFormId())
                .title(entity.getTitle())
                .formUrl(entity.getFormUrl())
                .sheetUrl(entity.getSheetUrl())
                .isActive(entity.getIsActive())
                .description(entity.getDescription())
                .recruitingStartDate(entity.getRecruitingStartDate())
                .recruitingEndDate(entity.getRecruitingEndDate())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    // Entity를 DTO로 변환 (지원서 개수 포함)
    public static GoogleFormResponse fromWithApplicationCount(GoogleForm entity, Long applicationCount) {
        return from(entity).toBuilder()
            .applicationCount(applicationCount)
            .build();
    }
}