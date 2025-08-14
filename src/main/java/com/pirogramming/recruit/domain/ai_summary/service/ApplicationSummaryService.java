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
     * Webhook에서 받은 폼 데이터를 요약하고 (formResponseId + email) 복합 유니크로 저장.
     */
    @Transactional
    public ApplicationSummary summarizeAndSaveFromWebhook(String formId,
                                                          String formResponseId,
                                                          String applicantName,
                                                          String applicantEmail,
                                                          Map<String, Object> formData) {

        if (summaryRepository.existsByFormResponseIdAndApplicantEmail(formResponseId, applicantEmail)) {
            throw new RecruitException(HttpStatus.CONFLICT, "이미 접수된 (지원서, 이메일) 조합입니다.");
        }

        // Map<String,Object> → List<ApplicationQuestionDto>
        List<ApplicationQuestionDto> questions = convertFormData(formData);

        // LLM 요약
        ApplicationSummaryDto summaryDto = processingService.processApplication(questions);

        // DTO를 저장용 Map으로 변환
        Map<String, String> items = flattenSummary(summaryDto);

        // 저장
        ApplicationSummary saved = summaryRepository.save(
                ApplicationSummary.builder()
                        .formId(formId)
                        .formResponseId(formResponseId)
                        .applicantName(applicantName)
                        .applicantEmail(applicantEmail)
                        .items(items)
                        .build()
        );

        return saved;
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
