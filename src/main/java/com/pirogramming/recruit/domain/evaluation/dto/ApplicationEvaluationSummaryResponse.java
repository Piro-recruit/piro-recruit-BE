package com.pirogramming.recruit.domain.evaluation.dto;

import java.util.List;

import lombok.Getter;

@Getter
public class ApplicationEvaluationSummaryResponse {

    private final Long applicationId;
    private final String applicantName;
    private final Double averageScore;
    private final Long evaluationCount;
    private final List<EvaluationResponse> evaluations;

    public ApplicationEvaluationSummaryResponse(Long applicationId, String applicantName, 
                                               Double averageScore, Long evaluationCount, 
                                               List<EvaluationResponse> evaluations) {
        this.applicationId = applicationId;
        this.applicantName = applicantName;
        this.averageScore = averageScore;
        this.evaluationCount = evaluationCount;
        this.evaluations = evaluations;
    }
}