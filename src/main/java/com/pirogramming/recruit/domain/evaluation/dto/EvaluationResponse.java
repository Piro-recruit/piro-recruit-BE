package com.pirogramming.recruit.domain.evaluation.dto;

import java.time.LocalDateTime;

import com.pirogramming.recruit.domain.evaluation.entity.Evaluation;

import lombok.Getter;

@Getter
public class EvaluationResponse {

    private final Long id;
    private final Long applicationId;
    private final String applicantName;
    private final Long evaluatorId;
    private final String evaluatorName;
    private final Integer score;
    private final String comment;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public EvaluationResponse(Evaluation evaluation) {
        this.id = evaluation.getId();
        this.applicationId = evaluation.getApplication().getId();
        this.applicantName = evaluation.getApplication().getApplicantName();
        this.evaluatorId = evaluation.getEvaluatorId();
        this.evaluatorName = evaluation.getEvaluatorName();
        this.score = evaluation.getScore();
        this.comment = evaluation.getComment();
        this.createdAt = evaluation.getCreatedAt();
        this.updatedAt = evaluation.getUpdatedAt();
    }
}