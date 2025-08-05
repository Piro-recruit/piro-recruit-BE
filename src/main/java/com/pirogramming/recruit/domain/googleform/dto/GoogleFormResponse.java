package com.pirogramming.recruit.domain.googleform.dto;

import com.pirogramming.recruit.domain.googleform.entity.GoogleForm;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GoogleFormResponse {

    private Long id;
    private String formId;
    private String title;
    private String formUrl;
    private String sheetUrl;
    private Boolean isActive;
    private String description;

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
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    // Entity를 DTO로 변환 (지원서 개수 포함)
    public static GoogleFormResponse fromWithApplicationCount(GoogleForm entity, Long applicationCount) {
        GoogleFormResponse response = from(entity);
        return GoogleFormResponse.builder()
                .id(response.getId())
                .formId(response.getFormId())
                .title(response.getTitle())
                .formUrl(response.getFormUrl())
                .sheetUrl(response.getSheetUrl())
                .isActive(response.getIsActive())
                .description(response.getDescription())
                .applicationCount(applicationCount)
                .createdAt(response.getCreatedAt())
                .updatedAt(response.getUpdatedAt())
                .build();
    }
}