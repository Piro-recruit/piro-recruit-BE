package com.pirogramming.recruit.domain.ai_summary.controller;

import com.pirogramming.recruit.domain.ai_summary.entity.ApplicationSummary;
import com.pirogramming.recruit.domain.ai_summary.repository.ApplicationSummaryRepository;
import com.pirogramming.recruit.global.exception.ApiRes;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/ai-summary")
@RequiredArgsConstructor
@Tag(name = "ApplicationSummary Query", description = "요약 결과 조회 API")
public class ApplicationSummaryQueryController {
    private final ApplicationSummaryRepository repository;

    @GetMapping("/by-form-response")
    @Operation(summary = "요약 단건 조회",
            description = "formResponseId + applicantEmail(복합 유니크)로 요약 결과를 조회합니다.")
    public ResponseEntity<ApiRes<ApplicationSummary>> getByFormResponseIdAndEmail(
            @RequestParam String formResponseId,
            @RequestParam String applicantEmail
    ) {
        Optional<ApplicationSummary> result = repository.findAll().stream()
                .filter(s -> formResponseId.equals(s.getFormResponseId())
                        && applicantEmail.equalsIgnoreCase(s.getApplicantEmail()))
                .findFirst();

        return result
                .map(s -> ResponseEntity.ok(ApiRes.success(s, "요약 결과 조회 성공")))
                .orElseGet(() -> ResponseEntity.ok(ApiRes.success(null, "해당 조합의 요약 결과가 없습니다.")));
    }
}
