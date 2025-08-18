package com.pirogramming.recruit.domain.ai_summary.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pirogramming.recruit.domain.ai_summary.infra.OpenAiChatClient;
import com.pirogramming.recruit.domain.ai_summary.service.AiBatchProcessingService;
import com.pirogramming.recruit.domain.ai_summary.service.ApplicationCacheService;
import com.pirogramming.recruit.global.exception.ApiRes;
import com.pirogramming.recruit.global.security.RequireAdmin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "AI 요약 모니터링", description = "AI 배치 처리 상태 및 성능 모니터링 API")
@RestController
@RequestMapping("/api/ai-summary/monitoring")
@RequiredArgsConstructor
public class AiSummaryMonitoringController {
    
    private final AiBatchProcessingService batchProcessingService;
    private final OpenAiChatClient openAiChatClient;
    private final ApplicationCacheService cacheService;
    
    @Operation(summary = "배치 처리 상태 조회", description = "AI 요약 배치 처리의 현재 상태와 통계를 조회합니다.")
    @GetMapping("/batch-status")
    @RequireAdmin
    public ApiRes<AiBatchProcessingService.BatchProcessingStats> getBatchStatus() {
        AiBatchProcessingService.BatchProcessingStats stats = batchProcessingService.getStats();
        return ApiRes.success(stats, "배치 처리 상태를 성공적으로 조회했습니다.");
    }
    
    @Operation(summary = "OpenAI API 통계 조회", description = "OpenAI API 호출 통계 및 동시성 제어 상태를 조회합니다.")
    @GetMapping("/openai-stats")
    @RequireAdmin
    public ApiRes<Map<String, Object>> getOpenAiStats() {
        Map<String, Object> stats = openAiChatClient.getApiStats();
        return ApiRes.success(stats, "OpenAI API 통계를 성공적으로 조회했습니다.");
    }
    
    @Operation(summary = "캐시 통계 조회", description = "AI 요약 캐시 상태를 조회합니다.")
    @GetMapping("/cache-stats")
    @RequireAdmin
    public ApiRes<ApplicationCacheService.CacheStats> getCacheStats() {
        ApplicationCacheService.CacheStats stats = cacheService.getCacheStats();
        return ApiRes.success(stats, "캐시 통계를 성공적으로 조회했습니다.");
    }
    
    @Operation(summary = "종합 대시보드", description = "AI 요약 시스템 전체 상태를 한 번에 조회합니다.")
    @GetMapping("/dashboard")
    @RequireAdmin
    public ApiRes<Map<String, Object>> getDashboard() {
        Map<String, Object> dashboard = Map.of(
            "batchProcessing", batchProcessingService.getStats(),
            "openAiApi", openAiChatClient.getApiStats(),
            "cache", cacheService.getCacheStats(),
            "timestamp", java.time.LocalDateTime.now()
        );
        return ApiRes.success(dashboard, "대시보드 데이터를 성공적으로 조회했습니다.");
    }
    
    @Operation(summary = "즉시 배치 처리 실행", description = "대기 중인 AI 요약 작업을 즉시 처리합니다.")
    @PostMapping("/trigger-batch")
    @RequireAdmin
    public ApiRes<String> triggerBatch() {
        batchProcessingService.triggerImmediateBatch();
        return ApiRes.success("배치 처리가 시작되었습니다.", "배치 처리를 수동으로 실행했습니다.");
    }
    
    @Operation(summary = "캐시 초기화", description = "AI 요약 캐시를 모두 초기화합니다.")
    @PostMapping("/clear-cache")
    @RequireAdmin
    public ApiRes<String> clearCache() {
        cacheService.clearCache();
        return ApiRes.success("캐시가 초기화되었습니다.", "캐시를 성공적으로 초기화했습니다.");
    }
}