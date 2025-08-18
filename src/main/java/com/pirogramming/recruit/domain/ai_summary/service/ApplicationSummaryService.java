package com.pirogramming.recruit.domain.ai_summary.service;

import com.pirogramming.recruit.domain.ai_summary.dto.ApplicationQuestionDto;
import com.pirogramming.recruit.domain.ai_summary.dto.ApplicationSummaryDto;
import com.pirogramming.recruit.domain.ai_summary.entity.ApplicationSummary;
import com.pirogramming.recruit.domain.ai_summary.repository.ApplicationSummaryRepository;
import com.pirogramming.recruit.global.exception.RecruitException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

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
        
        return formData.entrySet().stream()
                .filter(entry -> isNumericPrefixed(entry.getKey())) // 숫자로 시작하는 질문만 필터링
                .sorted((e1, e2) -> compareNumericPrefix(e1.getKey(), e2.getKey())) // 숫자 기준 오름차순 정렬
                .map(e -> new ApplicationQuestionDto(e.getKey(), Objects.toString(e.getValue(), "")))
                .collect(Collectors.toList());
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
     * 문자열 앞부분의 숫자를 추출
     */
    private int extractLeadingNumber(String key) {
        StringBuilder numStr = new StringBuilder();
        for (char c : key.toCharArray()) {
            if (Character.isDigit(c)) {
                numStr.append(c);
            } else {
                break;
            }
        }
        return Integer.parseInt(numStr.toString());
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

        map.put("overallSummary", n(dto.getOverallSummary()));
        map.put("experience", n(dto.getExperience()));
        map.put("motivation", n(dto.getMotivation()));
        map.put("scoreOutOf100", String.valueOf(dto.getScoreOutOf100()));

        // 리스트는 줄바꿈으로 합쳐 저장
        if (dto.getKeyStrengths() != null) {
            map.put("keyStrengths", String.join("\n", dto.getKeyStrengths()));
        }
        if (dto.getTechnicalSkills() != null) {
            map.put("technicalSkills", String.join(", ", dto.getTechnicalSkills()));
        }
        return map;
    }

    private String n(String s) { return s == null ? "" : s; }
}
