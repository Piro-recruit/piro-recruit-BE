package com.pirogramming.recruit.domain.webhook.dto;

import java.util.List;

import com.pirogramming.recruit.domain.webhook.entity.WebhookApplication;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "지원서 일괄 합격 상태 변경 요청")
public class BatchPassStatusUpdateRequest {

    @NotEmpty(message = "지원서 ID 목록은 필수입니다")
    @Schema(description = "변경할 지원서 ID 목록", example = "[1, 2, 3, 4, 5]")
    private List<Long> applicationIds;

    @NotNull(message = "합격 상태는 필수입니다")
    @Schema(description = "변경할 합격 상태", example = "FIRST_PASS")
    private WebhookApplication.PassStatus passStatus;

    public BatchPassStatusUpdateRequest(List<Long> applicationIds, WebhookApplication.PassStatus passStatus) {
        this.applicationIds = applicationIds;
        this.passStatus = passStatus;
    }
}