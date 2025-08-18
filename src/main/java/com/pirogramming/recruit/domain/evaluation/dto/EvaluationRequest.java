package com.pirogramming.recruit.domain.evaluation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "평가 생성 요청 DTO")
public class EvaluationRequest {

    @Schema(description = "평가할 지원서 ID", example = "1", required = true)
    @NotNull(message = "지원서 ID는 필수입니다. 평가할 지원서를 선택해주세요.")
    private Long applicationId;

    @Schema(description = "평가 점수 (0~100점)", example = "85", minimum = "0", maximum = "100", required = true)
    @NotNull(message = "점수는 필수입니다. 0점부터 100점까지 입력해주세요.")
    @Min(value = 0, message = "점수는 0점 이상이어야 합니다. 현재 입력된 값이 너무 낮습니다.")
    @Max(value = 100, message = "점수는 100점 이하여야 합니다. 현재 입력된 값이 너무 높습니다.")
    private Integer score;

    @Schema(description = "평가 코멘트", example = "전반적으로 우수한 지원서입니다. 기술적 역량이 뛰어나고 프로젝트 경험이 풍부합니다.")
    private String comment;

    public EvaluationRequest(Long applicationId, Integer score, String comment) {
        this.applicationId = applicationId;
        this.score = score;
        this.comment = comment;
    }
}