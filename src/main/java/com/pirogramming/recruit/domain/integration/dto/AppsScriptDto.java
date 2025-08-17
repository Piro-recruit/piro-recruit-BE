package com.pirogramming.recruit.domain.integration.dto;

import java.time.LocalDateTime;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class AppsScriptDto {

    // CSV 내보내기 요청 DTO
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ExportRequest {
        private Long googleFormId;
        private String exportType; // "applicants", "admins", "all"
        private Map<String, Object> options;
    }

    // CSV 내보내기 응답 DTO
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ExportResponse {
        private String downloadUrl;
        private String filename;
        private long recordCount;
        private LocalDateTime generatedAt;
        private String status; // "success", "failed"
        private String message;
    }

    // Apps Script 연결 테스트 응답 DTO
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ConnectionTestResponse {
        private boolean connected;
        private String message;
        private LocalDateTime testedAt;
        private String version; // Apps Script 버전 정보
    }

    // CSV 미리보기 요청 DTO
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PreviewRequest {
        private Long googleFormId;
        private String dataType; // "applicants", "admins"
        private int limit;
    }

    // 통계 정보 응답 DTO
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StatisticsResponse {
        private Long googleFormId;
        private long totalApplicantCount;
        private long totalAdminCount;
        private long activeAdminCount;
        private int currentRecruitmentLevel;
        private LocalDateTime lastUpdated;
        private Map<String, Object> additionalStats;
    }
}