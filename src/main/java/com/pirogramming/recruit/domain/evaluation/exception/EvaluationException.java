package com.pirogramming.recruit.domain.evaluation.exception;

import org.springframework.http.HttpStatus;

import com.pirogramming.recruit.global.exception.RecruitException;
import com.pirogramming.recruit.global.exception.code.ErrorCode;

public class EvaluationException extends RecruitException {

    public EvaluationException(ErrorCode errorCode) {
        super(HttpStatus.BAD_REQUEST, errorCode);
    }

    public EvaluationException(HttpStatus status, ErrorCode errorCode) {
        super(status, errorCode);
    }

    public EvaluationException(HttpStatus status, ErrorCode errorCode, String customMessage) {
        super(status, errorCode, customMessage);
    }

    // 자주 사용되는 예외들을 위한 팩토리 메서드들
    public static EvaluationException notFound(Long evaluationId) {
        return new EvaluationException(
            HttpStatus.NOT_FOUND, 
            ErrorCode.EVALUATION_NOT_FOUND, 
            String.format("평가 ID %d를 찾을 수 없습니다.", evaluationId)
        );
    }

    public static EvaluationException alreadyExists(Long applicationId, String evaluatorName) {
        return new EvaluationException(
            HttpStatus.CONFLICT, 
            ErrorCode.EVALUATION_ALREADY_EXISTS, 
            String.format("평가자 '%s'는 이미 지원서 ID %d에 대한 평가를 등록하셨습니다.", evaluatorName, applicationId)
        );
    }

    public static EvaluationException permissionDenied(Long evaluationId, String evaluatorName) {
        return new EvaluationException(
            HttpStatus.FORBIDDEN, 
            ErrorCode.EVALUATION_PERMISSION_DENIED, 
            String.format("평가자 '%s'는 평가 ID %d에 대한 권한이 없습니다. 본인이 작성한 평가만 수정/삭제할 수 있습니다.", evaluatorName, evaluationId)
        );
    }

    public static EvaluationException applicationNotFound(Long applicationId) {
        return new EvaluationException(
            HttpStatus.NOT_FOUND, 
            ErrorCode.EVALUATION_APPLICATION_NOT_FOUND, 
            String.format("지원서 ID %d를 찾을 수 없습니다.", applicationId)
        );
    }

    public static EvaluationException evaluatorNotFound(Long evaluatorId) {
        return new EvaluationException(
            HttpStatus.NOT_FOUND, 
            ErrorCode.EVALUATION_EVALUATOR_NOT_FOUND, 
            String.format("평가자 ID %d를 찾을 수 없습니다.", evaluatorId)
        );
    }

    public static EvaluationException invalidScore(Integer score) {
        return new EvaluationException(
            HttpStatus.BAD_REQUEST, 
            ErrorCode.EVALUATION_INVALID_SCORE, 
            String.format("유효하지 않은 점수입니다: %d. 점수는 0점 이상 100점 이하여야 합니다.", score)
        );
    }
}