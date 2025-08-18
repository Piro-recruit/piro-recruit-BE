package com.pirogramming.recruit.domain.evaluation.dto;

import java.time.LocalDateTime;

import com.pirogramming.recruit.domain.evaluation.entity.Evaluation;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "평가 응답 DTO")
public class EvaluationResponse {

    @Schema(description = "평가 ID", example = "1")
    private final Long id;
    
    @Schema(description = "지원서 ID", example = "1")
    private final Long applicationId;
    
    @Schema(description = "지원자 이름", example = "홍길동")
    private final String applicantName;
    
    @Schema(description = "평가자 ID", example = "1")
    private final Long evaluatorId;
    
    @Schema(description = "평가자 이름", example = "평가자1")
    private final String evaluatorName;
    
    @Schema(description = "평가 점수", example = "85")
    private final Integer score;
    
    @Schema(description = "평가 코멘트", example = "전반적으로 우수한 지원서입니다.")
    private final String comment;
    
    @Schema(description = "평가 생성 시간", example = "2024-01-01T10:00:00")
    private final LocalDateTime createdAt;
    
    @Schema(description = "평가 수정 시간", example = "2024-01-01T10:30:00")
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