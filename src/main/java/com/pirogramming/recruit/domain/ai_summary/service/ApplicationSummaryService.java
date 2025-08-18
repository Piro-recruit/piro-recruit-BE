package com.pirogramming.recruit.domain.ai_summary.service;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pirogramming.recruit.domain.ai_summary.dto.ApplicationQuestionDto;
import com.pirogramming.recruit.domain.ai_summary.dto.ApplicationSummaryDto;
import com.pirogramming.recruit.domain.ai_summary.entity.ApplicationSummary;
import com.pirogramming.recruit.domain.ai_summary.repository.ApplicationSummaryRepository;
import com.pirogramming.recruit.domain.ai_summary.util.InputValidationUtil;
import com.pirogramming.recruit.domain.ai_summary.util.TextSanitizerUtil;
import com.pirogramming.recruit.global.exception.RecruitException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApplicationSummaryService {
    private final ApplicationProcessingService processingService;
    private final ApplicationSummaryRepository summaryRepository;

    /**
     * Webhook에서 받은 폼 데이터로 AI 요약 레코드를 즉시 생성 (PENDING 상태)
     * 실제 AI 처리는 비동기로 수행
     */
    @Transactional
    public ApplicationSummary createPendingSummaryFromWebhook(com.pirogramming.recruit.domain.webhook.entity.WebhookApplication webhookApplication) {
        // 이미 AI 요약이 존재하는지 확인
        if (webhookApplication.getApplicationSummary() != null) {
            throw new RecruitException(HttpStatus.CONFLICT, "이미 AI 요약이 생성된 지원서입니다.");
        }

        // PENDING 상태로 즉시 저장 (AI 처리는 나중에)
        ApplicationSummary pendingSummary = summaryRepository.save(
                ApplicationSummary.builder()
                        .webhookApplication(webhookApplication)
                        .processingStatus(ApplicationSummary.ProcessingStatus.PENDING)
                        .build()
        );

        log.info("Created pending AI summary for application ID: {}", webhookApplication.getId());
        return pendingSummary;
    }

    /**
     * 기존 방식 유지 (동기 처리) - 테스트나 즉시 처리가 필요한 경우
     */
    @Transactional
    public ApplicationSummary summarizeAndSaveFromWebhookSync(com.pirogramming.recruit.domain.webhook.entity.WebhookApplication webhookApplication) {
        // 이미 AI 요약이 존재하는지 확인
        if (webhookApplication.getApplicationSummary() != null) {
            throw new RecruitException(HttpStatus.CONFLICT, "이미 AI 요약이 생성된 지원서입니다.");
        }

        // Map<String,Object> → List<ApplicationQuestionDto> (숫자로 시작하는 질문만 처리, 오름차순 정렬)
        List<ApplicationQuestionDto> questions = convertFormDataWithNumericFilter(webhookApplication.getFormData());

        // LLM 요약
        ApplicationSummaryDto summaryDto = processingService.processApplication(questions);

        // DTO를 저장용 Map으로 변환
        Map<String, String> items = flattenSummary(summaryDto);

        // 저장
        ApplicationSummary saved = summaryRepository.save(
                ApplicationSummary.builder()
                        .webhookApplication(webhookApplication)
                        .items(items)
                        .processingStatus(ApplicationSummary.ProcessingStatus.COMPLETED)
                        .processingStartedAt(java.time.LocalDateTime.now().minusSeconds(30))
                        .processingCompletedAt(java.time.LocalDateTime.now())
                        .build()
        );

        return saved;
    }

    /**
     * PENDING 상태의 요약을 실제 AI로 처리
     */
    @Transactional
    public void processAiSummary(ApplicationSummary summary) {
        if (summary.getProcessingStatus() != ApplicationSummary.ProcessingStatus.PENDING) {
            log.warn("Attempted to process non-pending summary ID: {}, status: {}", 
                summary.getId(), summary.getProcessingStatus());
            return;
        }

        try {
            // 처리 시작 표시
            summary.markAsProcessing();
            summaryRepository.save(summary);

            // 질문 데이터 변환 (WebhookApplication은 이미 fetch됨)
            List<ApplicationQuestionDto> questions = convertFormDataWithNumericFilter(
                summary.getWebhookApplication().getFormData());

            // AI 처리
            ApplicationSummaryDto summaryDto = processingService.processApplication(questions);

            // 결과 저장 (items 컬렉션이 이미 fetch되어 LazyInitializationException 방지됨)
            Map<String, String> items = flattenSummary(summaryDto);
            summary.getItems().clear();
            summary.getItems().putAll(items);
            
            // 완료 처리
            summary.markAsCompleted();
            summaryRepository.save(summary);

            log.info("Successfully processed AI summary for application ID: {}", 
                summary.getWebhookApplication().getId());

        } catch (Exception e) {
            // 실패 처리
            String errorMessage = "AI 처리 중 오류 발생: " + e.getMessage();
            summary.markAsFailed(errorMessage);
            summaryRepository.save(summary);

            log.error("Failed to process AI summary for application ID: {}", 
                summary.getId(), e);
        }
    }

    /**
     * 숫자로 시작하는 formData 질문들만 필터링하고 오름차순 정렬하여 변환
     */
    private List<ApplicationQuestionDto> convertFormDataWithNumericFilter(Map<String, Object> formData) {
        if (formData == null) return Collections.emptyList();
        
        List<ApplicationQuestionDto> questions = formData.entrySet().stream()
                .filter(entry -> isNumericPrefixed(entry.getKey())) // 숫자로 시작하는 질문만 필터링
                .filter(entry -> InputValidationUtil.isValidFormEntry(entry.getKey(), entry.getValue())) // 입력 검증
                .sorted((e1, e2) -> compareNumericPrefix(e1.getKey(), e2.getKey())) // 숫자 기준 오름차순 정렬
                .map(e -> new ApplicationQuestionDto(
                        TextSanitizerUtil.sanitizeInput(e.getKey()), 
                        TextSanitizerUtil.sanitizeInput(Objects.toString(e.getValue(), ""))))
                .collect(Collectors.toList());
        
        // 전체 프롬프트 크기 제한 (DoS 방지)
        int totalLength = questions.stream()
                .mapToInt(q -> q.getQuestion().length() + q.getAnswer().length())
                .sum();
        
        if (totalLength > 15000) { // 전체 합계 15KB 제한
            throw new RecruitException(HttpStatus.BAD_REQUEST, "전체 지원서 내용이 너무 깁니다. 답변을 줄여주세요.");
        }
        
        return questions;
    }

    

    /**
     * 문자열이 숫자로 시작하는지 확인
     */
    private boolean isNumericPrefixed(String key) {
        if (key == null || key.isEmpty()) return false;
        return Character.isDigit(key.charAt(0));
    }

    /**
     * 숫자 접두사를 기준으로 비교 (오름차순)
     */
    private int compareNumericPrefix(String key1, String key2) {
        try {
            // 숫자 부분만 추출하여 비교
            int num1 = extractLeadingNumber(key1);
            int num2 = extractLeadingNumber(key2);
            return Integer.compare(num1, num2);
        } catch (NumberFormatException e) {
            // 숫자 추출 실패 시 문자열 비교로 폴백
            return key1.compareTo(key2);
        }
    }

    /**
     * 문자열 앞부분의 숫자를 추출 (안전한 버전)
     */
    private int extractLeadingNumber(String key) {
        if (key == null || key.isEmpty()) {
            return 0;
        }
        
        StringBuilder numStr = new StringBuilder();
        for (char c : key.toCharArray()) {
            if (Character.isDigit(c)) {
                numStr.append(c);
            } else {
                break;
            }
        }
        
        // 숫자가 없는 경우 0 반환
        if (numStr.length() == 0) {
            return 0;
        }
        
        // Integer overflow 방지
        String numberString = numStr.toString();
        if (numberString.length() > 9) { // Integer.MAX_VALUE는 10자리이므로 9자리까지만 허용
            return Integer.MAX_VALUE;
        }
        
        try {
            return Integer.parseInt(numberString);
        } catch (NumberFormatException e) {
            // 예상치 못한 NumberFormatException 발생 시 0 반환
            return 0;
        }
    }

    /**
     * ApplicationSummaryDto를 저장하기 쉬운 K/V로 평탄화
     */
    private Map<String, String> flattenSummary(ApplicationSummaryDto dto) {
        Map<String, String> map = new LinkedHashMap<>();
        if (dto == null) return map;

        map.put("scoreOutOf100", String.valueOf(dto.getScoreOutOf100()));
        map.put("scoreReason", n(dto.getScoreReason()));

        // 질문별 요약을 JSON 형태로 저장
        if (dto.getQuestionSummaries() != null) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                map.put("questionSummaries", mapper.writeValueAsString(dto.getQuestionSummaries()));
            } catch (Exception e) {
                map.put("questionSummaries", "[]"); // 오류 시 빈 배열
            }
        }
        
        return map;
    }

    private String n(String s) { return s == null ? "" : s; }

    /**
     * WebhookApplication ID로 요약 조회
     */
    @Transactional(readOnly = true)
    public ApplicationSummary getByWebhookApplicationId(Long webhookApplicationId) {
        if (webhookApplicationId == null || webhookApplicationId <= 0) {
            throw new RecruitException(HttpStatus.BAD_REQUEST, "유효하지 않은 WebhookApplication ID입니다.");
        }

        return summaryRepository.findByWebhookApplicationId(webhookApplicationId)
                .orElseThrow(() -> new RecruitException(HttpStatus.NOT_FOUND, "해당 지원서의 AI 요약이 존재하지 않습니다."));
    }

    /**
     * 모든 AI 요약 조회 (최신순)
     */
    @Transactional(readOnly = true)
    public List<ApplicationSummary> getAllSummaries() {
        return summaryRepository.findAllByOrderByCreatedAtDesc();
    }

    /**
     * AI 요약 ID로 조회
     */
    @Transactional(readOnly = true)
    public ApplicationSummary getSummaryById(Long summaryId) {
        if (summaryId == null || summaryId <= 0) {
            throw new RecruitException(HttpStatus.BAD_REQUEST, "유효하지 않은 요약 ID입니다.");
        }

        return summaryRepository.findById(summaryId)
                .orElseThrow(() -> new RecruitException(HttpStatus.NOT_FOUND, "해당 AI 요약이 존재하지 않습니다."));
    }
}
