package com.pirogramming.recruit.domain.ai_summary.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import com.pirogramming.recruit.domain.ai_summary.dto.ApplicationQuestionDto;
import com.pirogramming.recruit.domain.ai_summary.dto.ApplicationSummaryDto;

import lombok.extern.slf4j.Slf4j;

/**
 * AI 요약 결과 캐싱 서비스
 * 동일한 질문 패턴에 대한 중복 API 호출 방지
 */
@Slf4j
@Service
public class ApplicationCacheService {
    
    private final Map<String, CachedSummary> cache = new ConcurrentHashMap<>();
    private static final int MAX_CACHE_SIZE = 1000;
    private static final int CACHE_EXPIRE_HOURS = 24;
    
    /**
     * 캐시된 요약 조회
     */
    public ApplicationSummaryDto getCachedSummary(List<ApplicationQuestionDto> questions) {
        String cacheKey = generateCacheKey(questions);
        CachedSummary cached = cache.get(cacheKey);
        
        if (cached != null && !isExpired(cached)) {
            log.info("Cache hit for questions pattern: {}", cacheKey.substring(0, Math.min(20, cacheKey.length())));
            return cached.summary;
        }
        
        // 만료된 캐시 제거
        if (cached != null && isExpired(cached)) {
            cache.remove(cacheKey);
        }
        
        return null;
    }
    
    /**
     * 요약 결과 캐싱
     */
    public void cacheSummary(List<ApplicationQuestionDto> questions, ApplicationSummaryDto summary) {
        // 캐시 크기 제한
        if (cache.size() >= MAX_CACHE_SIZE) {
            evictOldestEntries();
        }
        
        String cacheKey = generateCacheKey(questions);
        cache.put(cacheKey, new CachedSummary(summary, LocalDateTime.now()));
        
        log.debug("Cached summary for questions pattern: {}", cacheKey.substring(0, Math.min(20, cacheKey.length())));
    }
    
    /**
     * 캐시 키 생성 (질문 패턴 기반)
     */
    private String generateCacheKey(List<ApplicationQuestionDto> questions) {
        if (questions == null || questions.isEmpty()) {
            return "empty";
        }
        
        StringBuilder keyBuilder = new StringBuilder();
        
        // 질문 수와 각 질문의 해시값 조합
        keyBuilder.append("count:").append(questions.size()).append("|");
        
        for (ApplicationQuestionDto question : questions) {
            // 질문 내용만으로 키 생성 (답변은 제외하여 유사 질문 패턴 캐싱)
            String questionText = normalizeQuestion(question.getQuestion());
            keyBuilder.append(questionText.hashCode()).append(",");
        }
        
        return keyBuilder.toString();
    }
    
    /**
     * 질문 정규화 (캐싱 효율성 향상)
     */
    private String normalizeQuestion(String question) {
        if (question == null) return "";
        
        return question.toLowerCase()
            .replaceAll("[\\d]+\\.", "") // 번호 제거 (1., 2., 등)
            .replaceAll("[\\s]+", " ") // 공백 정규화
            .trim();
    }
    
    /**
     * 캐시 만료 검사
     */
    private boolean isExpired(CachedSummary cached) {
        return cached.createdAt.isBefore(LocalDateTime.now().minusHours(CACHE_EXPIRE_HOURS));
    }
    
    /**
     * 오래된 캐시 엔트리 제거 (LRU 방식)
     */
    private void evictOldestEntries() {
        int toRemove = cache.size() - MAX_CACHE_SIZE + 100; // 여유 공간 확보
        
        cache.entrySet().stream()
            .sorted((e1, e2) -> e1.getValue().createdAt.compareTo(e2.getValue().createdAt))
            .limit(toRemove)
            .map(Map.Entry::getKey)
            .forEach(cache::remove);
        
        log.info("Evicted {} old cache entries", toRemove);
    }
    
    /**
     * 캐시 통계 조회 (모니터링용)
     */
    public CacheStats getCacheStats() {
        long expiredCount = cache.values().stream()
            .mapToLong(cached -> isExpired(cached) ? 1 : 0)
            .sum();
        
        return new CacheStats(cache.size(), expiredCount);
    }
    
    /**
     * 캐시 초기화 (필요 시)
     */
    public void clearCache() {
        cache.clear();
        log.info("Cache cleared");
    }
    
    /**
     * 캐시된 요약 데이터
     */
    private static class CachedSummary {
        final ApplicationSummaryDto summary;
        final LocalDateTime createdAt;
        
        CachedSummary(ApplicationSummaryDto summary, LocalDateTime createdAt) {
            this.summary = summary;
            this.createdAt = createdAt;
        }
    }
    
    /**
     * 캐시 통계
     */
    public static class CacheStats {
        public final int totalEntries;
        public final long expiredEntries;
        
        CacheStats(int totalEntries, long expiredEntries) {
            this.totalEntries = totalEntries;
            this.expiredEntries = expiredEntries;
        }
    }
}