package com.pirogramming.recruit.domain.recruitment.dto;

import com.pirogramming.recruit.domain.recruitment.entity.Recruitment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecruitmentResponse {

    private Long id;
    private String title;
    private String description;
    private LocalDate startDate;
    private LocalDate endDate;
    private String googleFormUrl;
    private String googleSheetUrl;
    private String status;
    private Boolean isActive;

    // 추가 정보
    private Boolean isApplicationPeriod; // 현재 지원 기간인지
    private Long applicationCount; // 지원서 개수

    // 메타데이터
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Entity를 DTO로 변환
    public static RecruitmentResponse from(Recruitment entity) {
        return RecruitmentResponse.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .startDate(entity.getStartDate())
                .endDate(entity.getEndDate())
                .googleFormUrl(entity.getGoogleFormUrl())
                .googleSheetUrl(entity.getGoogleSheetUrl())
                .status(entity.getStatus().name())
                .isActive(entity.getIsActive())
                .isApplicationPeriod(entity.isApplicationPeriod())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    // Entity를 DTO로 변환 (지원서 개수 포함)
    public static RecruitmentResponse fromWithApplicationCount(Recruitment entity, Long applicationCount) {
        RecruitmentResponse response = from(entity);
        return RecruitmentResponse.builder()
                .id(response.getId())
                .title(response.getTitle())
                .description(response.getDescription())
                .startDate(response.getStartDate())
                .endDate(response.getEndDate())
                .googleFormUrl(response.getGoogleFormUrl())
                .googleSheetUrl(response.getGoogleSheetUrl())
                .status(response.getStatus())
                .isActive(response.getIsActive())
                .isApplicationPeriod(response.getIsApplicationPeriod())
                .applicationCount(applicationCount) // 지원서 개수 추가
                .createdAt(response.getCreatedAt())
                .updatedAt(response.getUpdatedAt())
                .build();
    }
}