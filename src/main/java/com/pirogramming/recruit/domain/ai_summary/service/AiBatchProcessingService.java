package com.pirogramming.recruit.domain.ai_summary.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import com.pirogramming.recruit.domain.ai_summary.entity.ApplicationSummary;
import com.pirogramming.recruit.domain.ai_summary.repository.ApplicationSummaryRepository;
import com.pirogramming.recruit.domain.googleform.entity.FormStatus;
import com.pirogramming.recruit.domain.googleform.repository.GoogleFormRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * AI 요약 배치 처리 서비스
 * 대기열의 PENDING 작업들을 주기적으로 처리하여 40개 동시 처리 목표 달성
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiBatchProcessingService {
    
    private final ApplicationSummaryRepository summaryRepository;
    private final ApplicationSummaryService summaryService;
    private final GoogleFormRepository googleFormRepository;
    
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private final ScheduledExecutorService batchProcessor = Executors.newScheduledThreadPool(10); // 배치 처리 스레드풀
    
    // 설정값들
    @Value("${ai.batch.processing.enabled:true}")
    private boolean batchProcessingEnabled;
    
    @Value("${ai.batch.processing.interval:5}")
    private int processingIntervalSeconds;
    
    @Value("${ai.batch.size:8}")
    private int batchSize;
    
    @Value("${ai.batch.retry.delay:300}")
    private int retryDelaySeconds;
    
    /**
     * 애플리케이션 시작 시 배치 처리 스케줄러 시작
     */
    @EventListener(ApplicationReadyEvent.class)
    public void startBatchProcessing() {
        if (!batchProcessingEnabled) {
            log.info("AI batch processing is disabled");
            return;
        }
        
        log.info("Starting AI batch processing service...");
        log.info("Batch size: {}, Processing interval: {}s, Retry delay: {}s", 
            batchSize, processingIntervalSeconds, retryDelaySeconds);
        
        // 주요 배치 처리 스케줄러 (PENDING 작업 처리)
        scheduler.scheduleAtFixedRate(
            this::processPendingBatch,
            10, // 10초 후 시작
            processingIntervalSeconds,
            TimeUnit.SECONDS
        );
        
        // 실패 작업 재시도 스케줄러
        scheduler.scheduleAtFixedRate(
            this::processFailedRetries,
            60, // 1분 후 시작
            retryDelaySeconds,
            TimeUnit.SECONDS
        );
        
        // 타임아웃된 작업 복구 스케줄러
        scheduler.scheduleAtFixedRate(
            this::recoverTimedOutTasks,
            30, // 30초 후 시작
            120, // 2분마다
            TimeUnit.SECONDS
        );
    }
    
    /**
     * PENDING 상태 작업들을 배치로 처리
     */
    public void processPendingBatch() {
        try {
            // 활성화된 구글 폼이 없으면 스킵 (리쿠르팅 기간이 아님)
            if (!googleFormRepository.existsByStatus(FormStatus.ACTIVE)) {
                log.debug("No active Google Form found, skipping AI batch processing");
                return;
            }
            // 1단계: ID만 먼저 조회하여 페이징 적용 (성능 최적화)
            PageRequest pageRequest = PageRequest.of(0, batchSize, Sort.by("createdAt").ascending());
            List<Long> pendingTaskIds = summaryRepository
                .findPendingTaskIds(ApplicationSummary.ProcessingStatus.PENDING, pageRequest);
            
            if (pendingTaskIds.isEmpty()) {
                log.debug("No pending AI summary tasks found");
                return;
            }
            
            // 2단계: 필요한 연관 데이터와 함께 조회
            List<ApplicationSummary> pendingTasks = summaryRepository
                .findByIdsWithAssociations(pendingTaskIds);
            
            log.info("Processing {} pending AI summary tasks", pendingTasks.size());
            
            // 각 작업을 비동기로 처리 (OpenAI API Semaphore가 동시성 제어)
            List<CompletableFuture<Void>> futures = pendingTasks.stream()
                .map(this::processTaskAsync)
                .toList();
            
            // 모든 작업 완료 대기 (최대 60초)
            CompletableFuture<Void> allTasks = CompletableFuture.allOf(
                futures.toArray(new CompletableFuture[0]));
            
            try {
                allTasks.get(60, TimeUnit.SECONDS);
                log.info("Completed processing {} AI summary tasks", pendingTasks.size());
            } catch (java.util.concurrent.TimeoutException e) {
                log.warn("Batch processing timed out after 60 seconds. Some tasks may still be running.");
            }
            
        } catch (Exception e) {
            log.error("Error in pending batch processing", e);
        }
    }
    
    /**
     * 개별 작업을 비동기로 처리
     */
    private CompletableFuture<Void> processTaskAsync(ApplicationSummary summary) {
        return CompletableFuture.runAsync(() -> {
            try {
                summaryService.processAiSummary(summary);
            } catch (Exception e) {
                log.error("Failed to process AI summary task ID: {}", summary.getId(), e);
            }
        }, batchProcessor);
    }
    
    /**
     * 실패한 작업들 재시도 처리
     */
    public void processFailedRetries() {
        try {
            // 활성화된 구글 폼이 없으면 스킵 (리쿠르팅 기간이 아님)
            if (!googleFormRepository.existsByStatus(FormStatus.ACTIVE)) {
                log.debug("No active Google Form found, skipping retry processing");
                return;
            }
            LocalDateTime retryThreshold = LocalDateTime.now().minusSeconds(retryDelaySeconds);
            List<ApplicationSummary> retryableTasks = summaryRepository
                .findRetryableFailed(retryThreshold);
            
            if (retryableTasks.isEmpty()) {
                log.debug("No retryable failed tasks found");
                return;
            }
            
            log.info("Retrying {} failed AI summary tasks", retryableTasks.size());
            
            for (ApplicationSummary task : retryableTasks) {
                // 다시 PENDING 상태로 변경하여 재처리 대상에 포함
                task.setProcessingStatus(ApplicationSummary.ProcessingStatus.PENDING);
                task.setErrorMessage(null);
                summaryRepository.save(task);
            }
            
        } catch (Exception e) {
            log.error("Error in retry processing", e);
        }
    }
    
    /**
     * 타임아웃된 작업들 복구 (5분 이상 PROCESSING 상태인 작업들)
     */
    public void recoverTimedOutTasks() {
        try {
            // 활성화된 구글 폼이 없으면 스킵 (리쿠르팅 기간이 아님)
            if (!googleFormRepository.existsByStatus(FormStatus.ACTIVE)) {
                log.debug("No active Google Form found, skipping timeout recovery");
                return;
            }
            LocalDateTime timeoutThreshold = LocalDateTime.now().minusMinutes(5);
            List<ApplicationSummary> timedOutTasks = summaryRepository
                .findTimedOutProcessing(timeoutThreshold);
            
            if (timedOutTasks.isEmpty()) {
                log.debug("No timed out tasks found");
                return;
            }
            
            log.warn("Recovering {} timed out AI summary tasks", timedOutTasks.size());
            
            for (ApplicationSummary task : timedOutTasks) {
                task.markAsFailed("Processing timeout (5분 초과)");
                summaryRepository.save(task);
            }
            
        } catch (Exception e) {
            log.error("Error in timeout recovery", e);
        }
    }
    
    /**
     * 즉시 배치 처리 실행 (수동 트리거용)
     */
    public void triggerImmediateBatch() {
        log.info("Manually triggering immediate batch processing");
        CompletableFuture.runAsync(this::processPendingBatch);
    }
    
    /**
     * 배치 처리 통계 조회
     */
    public BatchProcessingStats getStats() {
        List<Object[]> statusStats = summaryRepository.getProcessingStatusStats();
        
        long pending = 0;
        long processing = 0;
        long completed = 0;
        long failed = 0;
        
        for (Object[] stat : statusStats) {
            String status = (String) stat[0];
            Long count = (Long) stat[1];
            
            switch (status) {
                case "PENDING" -> pending = count;
                case "PROCESSING" -> processing = count;
                case "COMPLETED" -> completed = count;
                case "FAILED" -> failed = count;
            }
        }
        
        return new BatchProcessingStats(pending, processing, completed, failed, 
            batchProcessingEnabled, batchSize, processingIntervalSeconds);
    }
    
    /**
     * 애플리케이션 종료 시 스케줄러 정리
     */
    public void shutdown() {
        log.info("Shutting down AI batch processing service");
        scheduler.shutdown();
        batchProcessor.shutdown();
        
        try {
            if (!scheduler.awaitTermination(30, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
            if (!batchProcessor.awaitTermination(30, TimeUnit.SECONDS)) {
                batchProcessor.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            batchProcessor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * 배치 처리 통계 DTO
     */
    public static class BatchProcessingStats {
        public final long pendingTasks;
        public final long processingTasks;
        public final long completedTasks;
        public final long failedTasks;
        public final boolean enabled;
        public final int batchSize;
        public final int intervalSeconds;
        public final long totalTasks;
        public final double completionRate;
        
        public BatchProcessingStats(long pending, long processing, long completed, long failed,
                                  boolean enabled, int batchSize, int intervalSeconds) {
            this.pendingTasks = pending;
            this.processingTasks = processing;
            this.completedTasks = completed;
            this.failedTasks = failed;
            this.enabled = enabled;
            this.batchSize = batchSize;
            this.intervalSeconds = intervalSeconds;
            this.totalTasks = pending + processing + completed + failed;
            this.completionRate = totalTasks > 0 ? (double) completed / totalTasks * 100 : 0;
        }
    }
}