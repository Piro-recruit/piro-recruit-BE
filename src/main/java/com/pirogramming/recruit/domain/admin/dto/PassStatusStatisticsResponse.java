package com.pirogramming.recruit.domain.admin.dto;

import java.time.LocalDateTime;
import java.util.Map;

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
@Schema(description = "합격 상태 통계 응답")
public class PassStatusStatisticsResponse {

    @Schema(description = "구글 폼 ID (전체 통계인 경우 null)")
    private Long googleFormId;

    @Schema(description = "합격 상태별 통계")
    private Map<WebhookApplication.PassStatus, Long> statusStatistics;

    @Schema(description = "전체 지원자 수")
    private Long totalCount;

    @Schema(description = "통계 생성 시간")
    private LocalDateTime generatedAt;

    public static PassStatusStatisticsResponse of(
            Long googleFormId,
            Map<WebhookApplication.PassStatus, Long> statistics) {

        Long total = statistics.values().stream().mapToLong(Long::longValue).sum();

        return PassStatusStatisticsResponse.builder()
                .googleFormId(googleFormId)
                .statusStatistics(statistics)
                .totalCount(total)
                .generatedAt(LocalDateTime.now())
                .build();
    }
}