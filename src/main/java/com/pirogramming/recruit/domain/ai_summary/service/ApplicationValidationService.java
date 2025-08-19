package com.pirogramming.recruit.domain.ai_summary.service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.pirogramming.recruit.domain.ai_summary.dto.ApplicationSummaryDto;
import com.pirogramming.recruit.domain.ai_summary.util.FallbackResponseUtil;
import com.pirogramming.recruit.domain.ai_summary.util.TextSanitizerUtil;

import lombok.extern.slf4j.Slf4j;

/**
 * AI 응답 검증 및 정제 서비스
 * ApplicationProcessingService에서 분리된 검증 로직
 */
@Slf4j
@Service
public class ApplicationValidationService {
    
    /**
     * 캐싱 가능한 유효한 결과인지 검사
     */
    public boolean isValidForCaching(ApplicationSummaryDto result) {
        if (result == null) return false;
        
        // 폴백 응답은 캐싱하지 않음
        if (result.getScoreOutOf100() == 0 && 
            result.getScoreReason().contains("오류")) {
            return false;
        }
        
        // 빈 요약은 캐싱하지 않음
        if (result.getQuestionSummaries() == null || 
            result.getQuestionSummaries().isEmpty()) {
            return false;
        }
        
        return true;
    }
    
    /**
     * 파싱된 요약 결과 검증 및 정제 (강화된 버전)
     */
    public ApplicationSummaryDto validateAndSanitizeSummary(ApplicationSummaryDto summary) {
        if (summary == null) {
            return FallbackResponseUtil.createFallbackSummary();
        }
        
        // 1. 점수 범위 및 일관성 검증
        int score = validateScore(summary.getScoreOutOf100());
        
        // 2. 질문별 요약 검증 및 정제
        List<ApplicationSummaryDto.QuestionSummaryDto> cleanQuestionSummaries = 
            validateAndCleanQuestionSummaries(summary.getQuestionSummaries());
        
        // 3. scoreReason 검증 및 정제
        String cleanScoreReason = validateAndCleanScoreReason(summary.getScoreReason(), score);
        
        // 4. 전체 내용 일관성 검사
        if (!isContentConsistent(cleanQuestionSummaries, score, cleanScoreReason)) {
            log.warn("Inconsistent AI response detected, applying corrections");
            score = adjustScoreForInconsistency(score);
        }
        
        ApplicationSummaryDto result = new ApplicationSummaryDto();
        result.setQuestionSummaries(cleanQuestionSummaries);
        result.setScoreOutOf100(score);
        result.setScoreReason(cleanScoreReason);
        return result;
    }
    
    /**
     * 점수 유효성 검증
     */
    private int validateScore(int score) {
        // 기본 범위 검증
        if (score < 0 || score > 100) {
            score = Math.max(0, Math.min(100, score));
        }
        
        // 비현실적 점수 검사 (너무 극단적인 점수 조정)
        if (score == 0 || score == 100) {
            log.info("Extreme score detected: {}, applying moderation", score);
            score = score == 0 ? 10 : 95; // 예외상황 제외하고 극단적 점수 완화
        }
        
        return score;
    }
    
    /**
     * 질문별 요약 검증 및 정제
     */
    private List<ApplicationSummaryDto.QuestionSummaryDto> validateAndCleanQuestionSummaries(
            List<ApplicationSummaryDto.QuestionSummaryDto> questionSummaries) {
        
        if (questionSummaries == null || questionSummaries.isEmpty()) {
            return List.of();
        }
        
        return questionSummaries.stream()
            .filter(Objects::nonNull)
            .map(this::sanitizeQuestionSummary)
            .filter(Objects::nonNull) // 정제 실패 제외
            .filter(q -> !q.getAiSummary().trim().isEmpty()) // 빈 요약 제외
            .limit(30) // DoS 방지
            .collect(Collectors.toList());
    }
    
    /**
     * 점수 근거 검증 및 정제
     */
    private String validateAndCleanScoreReason(String scoreReason, int score) {
        String cleaned = TextSanitizerUtil.sanitize(scoreReason);
        
        if (cleaned == null || cleaned.trim().isEmpty()) {
            return "점수 근거를 제공할 수 없습니다.";
        }
        
        // 점수 근거의 기본 구조 검증
        if (!isValidScoreReasonFormat(cleaned)) {
            log.warn("Invalid score reason format detected");
            return "점수 근거 형식이 올바르지 않아 자동 검토가 필요합니다.";
        }
        
        return cleaned;
    }
    
    /**
     * 점수 근거 형식 유효성 검사
     */
    private boolean isValidScoreReasonFormat(String scoreReason) {
        // 기본적인 평가 영역들이 포함되어 있는지 검사
        String[] expectedKeywords = {"열정", "협업", "기술", "성장"};
        String lowerReason = scoreReason.toLowerCase();
        
        int foundKeywords = 0;
        for (String keyword : expectedKeywords) {
            if (lowerReason.contains(keyword)) {
                foundKeywords++;
            }
        }
        
        // 최소 2개 이상의 평가 영역이 언급되어야 함
        return foundKeywords >= 2;
    }
    
    /**
     * 전체 내용 일관성 검사
     */
    private boolean isContentConsistent(List<ApplicationSummaryDto.QuestionSummaryDto> summaries, 
                                       int score, String scoreReason) {
        // 1. 요약 개수와 점수의 일관성
        if (summaries.isEmpty() && score > 20) {
            return false; // 요약이 없는데 높은 점수
        }
        
        // 2. 점수와 근거의 일관성 (단순 검사)
        if (score >= 80 && scoreReason.contains("부족")) {
            return false; // 높은 점수인데 부족하다는 평가
        }
        
        if (score <= 30 && scoreReason.contains("우수")) {
            return false; // 낮은 점수인데 우수하다는 평가
        }
        
        return true;
    }
    
    /**
     * 비일관성 발견 시 점수 조정
     */
    private int adjustScoreForInconsistency(int originalScore) {
        // 비일관성이 발견된 경우 중간 점수대로 조정
        if (originalScore >= 80) {
            return 70; // 높은 점수는 중간으로
        } else if (originalScore <= 30) {
            return 40; // 낮은 점수도 중간으로
        }
        return originalScore; // 중간 점수는 유지
    }
    
    /**
     * 개별 질문 요약 정제 (강화된 버전)
     */
    private ApplicationSummaryDto.QuestionSummaryDto sanitizeQuestionSummary(ApplicationSummaryDto.QuestionSummaryDto questionSummary) {
        if (questionSummary == null) {
            return null;
        }
        
        String cleanQuestion = TextSanitizerUtil.sanitize(questionSummary.getQuestion());
        String cleanAiSummary = TextSanitizerUtil.sanitize(questionSummary.getAiSummary());
        
        // 빈 내용 검사
        if (cleanQuestion.trim().isEmpty() || cleanAiSummary.trim().isEmpty()) {
            return null;
        }
        
        // AI 요약이 원본 답변을 그대로 복사한 경우 감지
        if (cleanAiSummary.length() > 500 && containsOriginalAnswer(cleanAiSummary)) {
            log.warn("AI summary appears to contain original answer, truncating");
            cleanAiSummary = truncateToSummary(cleanAiSummary);
        }
        
        // 부적절한 내용 감지
        if (containsInappropriateContent(cleanAiSummary)) {
            log.warn("Inappropriate content detected in AI summary");
            cleanAiSummary = "요약 내용에 부적절한 내용이 감지되어 수동 검토가 필요합니다.";
        }
        
        return new ApplicationSummaryDto.QuestionSummaryDto(cleanQuestion, cleanAiSummary);
    }
    
    /**
     * 원본 답변 포함 여부 감지
     */
    private boolean containsOriginalAnswer(String aiSummary) {
        // 요약이 아닌 원본 답변을 그대로 복사한 경우를 감지
        // 이는 간단한 휴리스틱으로 정확하지 않을 수 있음
        
        // 1. 반복적인 문장 구조 감지
        String[] longSentenceIndicators = {
            "저는", "제가", "바랍니다", "생각합니다", "경험이 있습니다"
        };
        
        int indicatorCount = 0;
        for (String indicator : longSentenceIndicators) {
            if (aiSummary.contains(indicator)) {
                indicatorCount++;
            }
        }
        
        return indicatorCount >= 3; // 3개 이상의 지시어가 있으면 원본 답변일 가능성
    }
    
    /**
     * 요약으로 잘라내기
     */
    private String truncateToSummary(String text) {
        // 첫 번째 문장만 추출하여 요약으로 사용
        String[] sentences = text.split("[.!?]");
        if (sentences.length > 0) {
            String firstSentence = sentences[0].trim();
            if (firstSentence.length() > 100) {
                return firstSentence.substring(0, 97) + "...";
            }
            return firstSentence + ".";
        }
        return "요약 생성 실패.";
    }
    
    /**
     * 부적절한 내용 감지
     */
    private boolean containsInappropriateContent(String content) {
        // 기본적인 부적절 내용 감지
        String[] inappropriatePatterns = {
            "개인정보", "전화번호", "주소", "이메일", 
            "비밀번호", "주민등록번호", "password", "email"
        };
        
        String lowerContent = content.toLowerCase();
        for (String pattern : inappropriatePatterns) {
            if (lowerContent.contains(pattern.toLowerCase())) {
                return true;
            }
        }
        
        return false;
    }
}