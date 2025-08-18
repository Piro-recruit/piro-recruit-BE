package com.pirogramming.recruit.domain.evaluation.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class EvaluationUpdateRequest {

    @NotNull(message = "점수는 필수입니다. 0점부터 100점까지 입력해주세요.")
    @Min(value = 0, message = "점수는 0점 이상이어야 합니다. 현재 입력된 값: ${validatedValue}")
    @Max(value = 100, message = "점수는 100점 이하여야 합니다. 현재 입력된 값: ${validatedValue}")
    private Integer score;

    private String comment;

    public EvaluationUpdateRequest(Integer score, String comment) {
        this.score = score;
        this.comment = comment;
    }
}