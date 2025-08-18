package com.pirogramming.recruit.domain.ai_summary.controller;

import com.pirogramming.recruit.domain.ai_summary.entity.ApplicationSummary;
import com.pirogramming.recruit.domain.ai_summary.repository.ApplicationSummaryRepository;
import com.pirogramming.recruit.global.exception.ApiRes;
import com.pirogramming.recruit.global.exception.code.ErrorCode;
import com.pirogramming.recruit.global.security.RequireAdmin;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/ai-summary")
@RequiredArgsConstructor
@RequireAdmin
@Tag(name = "ApplicationSummary Query", description = "AI 요약 결과 조회 API")
public class ApplicationSummaryQueryController {
    private final ApplicationSummaryRepository repository;

    @GetMapping("/webhook-application/{webhookApplicationId}")
    @Operation(summary = "WebhookApplication ID로 요약 조회",
            description = "WebhookApplication ID를 기준으로 AI 요약 결과를 조회합니다.")
    public ResponseEntity<ApiRes<ApplicationSummary>> getByWebhookApplicationId(
            @Parameter(description = "WebhookApplication ID") @PathVariable Long webhookApplicationId
    ) {
        // 입력 검증
        if (webhookApplicationId == null || webhookApplicationId <= 0) {
            return ResponseEntity.badRequest()
                    .body(ApiRes.failure(HttpStatus.BAD_REQUEST, "유효하지 않은 WebhookApplication ID입니다.", ErrorCode.INVALID_INPUT_VALUE));
        }

        Optional<ApplicationSummary> result = repository.findByWebhookApplicationId(webhookApplicationId);

        return result
                .map(s -> ResponseEntity.ok(ApiRes.success(s, "요약 결과 조회 성공")))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiRes.failure(HttpStatus.NOT_FOUND, "해당 지원서의 AI 요약이 존재하지 않습니다.", ErrorCode.RESOURCE_NOT_FOUND)));
    }

    @GetMapping("/all")
    @Operation(summary = "모든 AI 요약 조회",
            description = "모든 AI 요약 결과를 최신순으로 조회합니다.")
    public ResponseEntity<ApiRes<List<ApplicationSummary>>> getAllSummaries() {
        List<ApplicationSummary> summaries = repository.findAllByOrderByCreatedAtDesc();
        return ResponseEntity.ok(ApiRes.success(summaries, summaries.size() + "개의 AI 요약을 조회했습니다."));
    }

    @GetMapping("/{summaryId}")
    @Operation(summary = "AI 요약 ID로 조회",
            description = "AI 요약 ID를 기준으로 결과를 조회합니다.")
    public ResponseEntity<ApiRes<ApplicationSummary>> getSummaryById(
            @Parameter(description = "AI 요약 ID") @PathVariable Long summaryId
    ) {
        // 입력 검증
        if (summaryId == null || summaryId <= 0) {
            return ResponseEntity.badRequest()
                    .body(ApiRes.failure(HttpStatus.BAD_REQUEST, "유효하지 않은 요약 ID입니다.", ErrorCode.INVALID_INPUT_VALUE));
        }

        Optional<ApplicationSummary> result = repository.findById(summaryId);

        return result
                .map(s -> ResponseEntity.ok(ApiRes.success(s, "요약 결과 조회 성공")))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiRes.failure(HttpStatus.NOT_FOUND, "해당 AI 요약이 존재하지 않습니다.", ErrorCode.RESOURCE_NOT_FOUND)));
    }
}
