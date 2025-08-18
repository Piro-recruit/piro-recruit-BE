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
import com.pirogramming.recruit.global.exception.RecruitException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ApplicationSummaryService {
    private final ApplicationProcessingService processingService;
    private final ApplicationSummaryRepository summaryRepository;

    /**
     * Webhook에서 받은 폼 데이터를 요약하고 WebhookApplication과 연결하여 저장.
     */
    @Transactional
    public ApplicationSummary summarizeAndSaveFromWebhook(com.pirogramming.recruit.domain.webhook.entity.WebhookApplication webhookApplication) {

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
                        .build()
        );

        return saved;
    }

    /**
     * 숫자로 시작하는 formData 질문들만 필터링하고 오름차순 정렬하여 변환
     */
    private List<ApplicationQuestionDto> convertFormDataWithNumericFilter(Map<String, Object> formData) {
        if (formData == null) return Collections.emptyList();
        
        List<ApplicationQuestionDto> questions = formData.entrySet().stream()
                .filter(entry -> isNumericPrefixed(entry.getKey())) // 숫자로 시작하는 질문만 필터링
                .filter(entry -> isValidFormEntry(entry.getKey(), entry.getValue())) // 입력 검증
                .sorted((e1, e2) -> compareNumericPrefix(e1.getKey(), e2.getKey())) // 숫자 기준 오름차순 정렬
                .map(e -> new ApplicationQuestionDto(
                        sanitizeInput(e.getKey()), 
                        sanitizeInput(Objects.toString(e.getValue(), ""))))
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
     * 폼 입력값 유효성 검증
     */
    private boolean isValidFormEntry(String key, Object value) {
        if (key == null || key.trim().isEmpty()) return false;
        if (value == null) return false;
        
        String valueStr = Objects.toString(value, "");
        
        // 길이 제한 (DoS 방지) - 각 질문/답변 당
        if (key.length() > 500 || valueStr.length() > 2000) {
            return false;
        }
        
        // 악성 패턴 검사 (기본적인 프롬프트 인젝션 방지)
        String[] suspiciousPatterns = {
                "ignore previous", "forget everything", "new instructions", 
                "system:", "assistant:", "user:", "You are now", "Act as",
                "\\n\\n", "---", "```", "<script", "</script>", 
                "javascript:", "data:", "vbscript:"
        };
        
        String lowerKey = key.toLowerCase();
        String lowerValue = valueStr.toLowerCase();
        
        for (String pattern : suspiciousPatterns) {
            if (lowerKey.contains(pattern) || lowerValue.contains(pattern)) {
                return false;
            }
        }
        
        return true;
    }

    /**
     * 입력값 sanitization (프롬프트 인젝션 방지)
     */
    private String sanitizeInput(String input) {
        if (input == null) return "";
        
        return input
                // 제어 문자 제거
                .replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", "")
                // 연속된 공백 정규화
                .replaceAll("\\s+", " ")
                // 특수 마크다운/프롬프트 패턴 이스케이프
                .replace("```", "'''")
                .replace("---", "—")
                .replace("###", "")
                .replace("**", "")
                // 앞뒤 공백 제거
                .trim();
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

    private List<ApplicationQuestionDto> convertFormData(Map<String, Object> formData) {
        if (formData == null) return Collections.emptyList();
        return formData.entrySet().stream()
                .map(e -> new ApplicationQuestionDto(e.getKey(), Objects.toString(e.getValue(), "")))
                .collect(Collectors.toList());
    }

    /**
     * ApplicationSummaryDto를 저장하기 쉬운 K/V로 평탄화
     */
    private Map<String, String> flattenSummary(ApplicationSummaryDto dto) {
        Map<String, String> map = new LinkedHashMap<>();
        if (dto == null) return map;

        map.put("scoreOutOf100", String.valueOf(dto.getScoreOutOf100()));

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
}
