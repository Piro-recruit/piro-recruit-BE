package com.pirogramming.recruit.domain.admin.dto;

import java.util.List;

import com.pirogramming.recruit.domain.webhook.entity.WebhookApplication;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "일괄 합격 상태 변경 요청")
public class AllPassStatusUpdateRequest {

    @NotEmpty(message = "지원서 ID 목록은 필수입니다")
    @Schema(description = "지원서 ID 목록", example = "[1, 2, 3, 4, 5]", required = true)
    private List<Long> applicationIds;

    @NotNull(message = "합격 상태는 필수입니다")
    @Schema(description = "합격 상태 (PENDING, FAILED, FIRST_PASS, FINAL_PASS)",
            example = "FIRST_PASS", required = true)
    private WebhookApplication.PassStatus passStatus;

    @Schema(description = "변경 사유 (선택사항)", example = "1차 서류 심사 일괄 처리")
    private String reason;

}
