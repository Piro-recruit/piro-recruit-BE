package com.pirogramming.recruit.domain.evaluation.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class EvaluationUpdateRequest {

    @NotNull(message = "점수는 필수입니다")
    @Min(value = 0, message = "점수는 0점 이상이어야 합니다")
    @Max(value = 100, message = "점수는 100점 이하여야 합니다")
    private Integer score;

    private String comment;

    public EvaluationUpdateRequest(Integer score, String comment) {
        this.score = score;
        this.comment = comment;
    }
}