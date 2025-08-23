package com.pirogramming.recruit.domain.webhook.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "지원서 일괄 합격 상태 변경 응답")
public class BatchPassStatusUpdateResponse {

    @Schema(description = "업데이트된 지원서 목록")
    private List<WebhookApplicationResponse> updatedApplications;

    @Schema(description = "총 업데이트된 개수", example = "5")
    private int totalUpdated;

    @Schema(description = "업데이트 실패한 개수", example = "0")
    private int failedCount;

    @Schema(description = "변경된 합격 상태", example = "FIRST_PASS")
    private String passStatus;

    public BatchPassStatusUpdateResponse(List<WebhookApplicationResponse> updatedApplications, String passStatus) {
        this.updatedApplications = updatedApplications;
        this.totalUpdated = updatedApplications.size();
        this.failedCount = 0; // 현재는 모두 성공하거나 예외 발생하는 구조
        this.passStatus = passStatus;
    }

    public static BatchPassStatusUpdateResponse of(List<WebhookApplicationResponse> updatedApplications, String passStatus) {
        return new BatchPassStatusUpdateResponse(updatedApplications, passStatus);
    }
}