package com.pirogramming.recruit.domain.ai_summary.util;

import java.util.List;

import org.springframework.http.HttpStatus;

import com.pirogramming.recruit.domain.ai_summary.dto.ApplicationQuestionDto;
import com.pirogramming.recruit.global.exception.RecruitException;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * 입력 검증 유틸리티
 * Controller 계층의 중복된 검증 로직 통합
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class InputValidationUtil {
    
    /**
     * ID 값 검증
     */
    public static void validateId(Long id, String fieldName) {
        if (id == null || id <= 0) {
            throw new RecruitException(HttpStatus.BAD_REQUEST, 
                String.format("유효하지 않은 %s입니다.", fieldName));
        }
    }
    
    /**
     * 질문 목록 기본 검증
     */
    public static void validateQuestionList(List<ApplicationQuestionDto> questions) {
        if (questions == null || questions.isEmpty()) {
            throw new RecruitException(HttpStatus.BAD_REQUEST, "질문 목록이 비어있습니다.");
        }
    }
    
    /**
     * 개별 질문/답변 검증
     */
    public static void validateQuestionContent(List<ApplicationQuestionDto> questions) {
        for (ApplicationQuestionDto question : questions) {
            validateSingleQuestion(question);
        }
    }
    
    /**
     * 단일 질문 검증
     */
    public static void validateSingleQuestion(ApplicationQuestionDto question) {
        if (question == null) {
            throw new RecruitException(HttpStatus.BAD_REQUEST, "질문 객체가 null입니다.");
        }
        
        if (question.getQuestion() == null || question.getQuestion().trim().isEmpty()) {
            throw new RecruitException(HttpStatus.BAD_REQUEST, "질문이 비어있습니다.");
        }
        
        if (question.getAnswer() == null || question.getAnswer().trim().isEmpty()) {
            throw new RecruitException(HttpStatus.BAD_REQUEST, "답변이 비어있습니다.");
        }
        
        if (question.getQuestion().length() > 500) {
            throw new RecruitException(HttpStatus.BAD_REQUEST, 
                "질문이 너무 깁니다 (최대 500자).");
        }
        
        if (question.getAnswer().length() > 5000) {
            throw new RecruitException(HttpStatus.BAD_REQUEST, 
                "답변이 너무 깁니다 (최대 5000자).");
        }
    }
    
    /**
     * 배치 크기 검증
     */
    public static void validateBatchSize(List<?> list, int maxSize, String itemName) {
        if (list != null && list.size() > maxSize) {
            throw new RecruitException(HttpStatus.BAD_REQUEST, 
                String.format("%s는 최대 %d개까지 가능합니다.", itemName, maxSize));
        }
    }
}