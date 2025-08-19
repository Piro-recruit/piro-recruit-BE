package com.pirogramming.recruit.domain.ai_summary.controller;

import com.pirogramming.recruit.domain.ai_summary.entity.ApplicationSummary;
import com.pirogramming.recruit.domain.ai_summary.service.ApplicationSummaryService;
import com.pirogramming.recruit.global.exception.ApiRes;
import com.pirogramming.recruit.global.security.RequireAdmin;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ai-summary")
@RequiredArgsConstructor
@RequireAdmin
@Tag(name = "ApplicationSummary Query", description = "AI 요약 결과 조회 API")
public class ApplicationSummaryQueryController {
    private final ApplicationSummaryService summaryService;

    @GetMapping("/webhook-application/{webhookApplicationId}")
    @Operation(summary = "WebhookApplication ID로 요약 조회",
            description = "WebhookApplication ID를 기준으로 AI 요약 결과를 조회합니다.")
    public ApiRes<ApplicationSummary> getByWebhookApplicationId(
            @Parameter(description = "WebhookApplication ID") @PathVariable Long webhookApplicationId
    ) {
        ApplicationSummary result = summaryService.getByWebhookApplicationId(webhookApplicationId);
        return ApiRes.success(result, "요약 결과 조회 성공");
    }

    @GetMapping("/all")
    @Operation(summary = "모든 AI 요약 조회",
            description = "모든 AI 요약 결과를 최신순으로 조회합니다.")
    public ApiRes<List<ApplicationSummary>> getAllSummaries() {
        List<ApplicationSummary> summaries = summaryService.getAllSummaries();
        return ApiRes.success(summaries, summaries.size() + "개의 AI 요약을 조회했습니다.");
    }

    @GetMapping("/{summaryId}")
    @Operation(summary = "AI 요약 ID로 조회",
            description = "AI 요약 ID를 기준으로 결과를 조회합니다.")
    public ApiRes<ApplicationSummary> getSummaryById(
            @Parameter(description = "AI 요약 ID") @PathVariable Long summaryId
    ) {
        ApplicationSummary result = summaryService.getSummaryById(summaryId);
        return ApiRes.success(result, "요약 결과 조회 성공");
    }
}
