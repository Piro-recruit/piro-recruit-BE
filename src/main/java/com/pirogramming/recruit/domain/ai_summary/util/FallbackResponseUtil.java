package com.pirogramming.recruit.domain.ai_summary.util;

import java.util.List;

import com.pirogramming.recruit.domain.ai_summary.dto.ApplicationSummaryDto;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Fallback 응답 생성 유틸리티
 * ApplicationProcessingService와 OpenAiChatClient의 중복된 fallback 로직 통합
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class FallbackResponseUtil {
    
    /**
     * AI 분석 실패 시 사용할 fallback DTO 생성
     */
    public static ApplicationSummaryDto createFallbackSummary() {
        ApplicationSummaryDto fallback = new ApplicationSummaryDto();
        fallback.setQuestionSummaries(List.of());
        fallback.setScoreOutOf100(0);
        fallback.setScoreReason("AI 분석 중 오류가 발생하여 점수 근거를 제공할 수 없습니다.");
        return fallback;
    }
    
    /**
     * OpenAI API 응답 실패 시 사용할 fallback JSON 문자열 생성
     */
    public static String createFallbackJson() {
        return """
            {
              "questionSummaries": [],
              "scoreOutOf100": 0,
              "scoreReason": "AI 분석 서비스 오류로 인해 평가를 완료할 수 없습니다. 수동 검토가 필요합니다."
            }
            """;
    }
    
    /**
     * 사용자 정의 메시지로 fallback DTO 생성
     */
    public static ApplicationSummaryDto createFallbackSummary(String customMessage) {
        ApplicationSummaryDto fallback = new ApplicationSummaryDto();
        fallback.setQuestionSummaries(List.of());
        fallback.setScoreOutOf100(0);
        fallback.setScoreReason(customMessage);
        return fallback;
    }
}