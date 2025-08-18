package com.pirogramming.recruit.domain.ai_summary.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * 텍스트 정제 및 보안 유틸리티
 * ApplicationProcessingService와 ApplicationSummaryService의 중복 로직 통합
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TextSanitizerUtil {
    
    private static final int DEFAULT_MAX_LENGTH = 1000;
    
    /**
     * 기본 텍스트 정제 (1000자 제한)
     */
    public static String sanitize(String text) {
        return sanitize(text, DEFAULT_MAX_LENGTH);
    }
    
    /**
     * 텍스트 정제 (길이 제한 및 유해 콘텐츠 제거)
     */
    public static String sanitize(String text, int maxLength) {
        if (text == null) return "";
        
        String cleaned = text
            .replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", "") // 제어 문자 제거 (탭, 줄바꿈 제외)
            .replaceAll("\\s+", " ") // 연속 공백 정규화
            .trim();
        
        // 길이 제한 적용
        if (cleaned.length() > maxLength && maxLength > 3) {
            cleaned = cleaned.substring(0, maxLength - 3) + "...";
        }
        
        return cleaned;
    }
    
    /**
     * 입력값 sanitization (프롬프트 인젝션 방지 포함)
     */
    public static String sanitizeInput(String input) {
        if (input == null) return "";
        
        String sanitized = input
            // 제어 문자 제거 (탭, 줄바꿈 제외)
            .replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", "")
            // 연속된 공백 정규화
            .replaceAll("\\s+", " ")
            // 특수 마크다운/프롬프트 패턴 이스케이프
            .replace("```", "'''")
            .replace("---", "—")
            .replace("###", "")
            .replace("**", "")
            .replace("<!--", "<comment>")
            .replace("-->", "</comment>")
            // 앞뒤 공백 제거
            .trim();
        
        return sanitized;
    }
    
    /**
     * 프롬프트 인젝션 위험 텍스트 감지
     */
    public static boolean containsPromptInjectionRisk(String input) {
        if (input == null || input.trim().isEmpty()) return false;
        
        String normalized = normalizeForDetection(input);
        
        // 1. 직접적인 명령어 패턴
        String[] directCommands = {
            "ignore", "forget", "disregard", "override", "replace", "update", "modify",
            "new instructions", "different task", "change role", "act as", "you are now",
            "pretend", "simulate", "roleplay", "behave as"
        };
        
        // 2. 시스템 메시지 패턴
        String[] systemPatterns = {
            "system:", "assistant:", "user:", "human:", "ai:", "gpt:", "model:",
            "[system]", "[assistant]", "[user]", "[human]", "<system>", "</system>"
        };
        
        // 3. 출력 형식 조작 패턴
        String[] outputPatterns = {
            "output only", "respond with", "answer with", "reply with", "return only",
            "don't include", "exclude", "omit", "skip", "bypass"
        };
        
        return containsAnyPattern(normalized, directCommands) ||
               containsAnyPattern(normalized, systemPatterns) ||
               containsAnyPattern(normalized, outputPatterns);
    }
    
    /**
     * 탐지를 위한 텍스트 정규화
     */
    private static String normalizeForDetection(String input) {
        return input.toLowerCase()
            .replaceAll("[\\s\\p{Punct}]+", " ") // 공백과 구두점을 공백으로 통일
            .replaceAll("\\s+", " ") // 연속 공백 제거
            .trim();
    }
    
    /**
     * 패턴 배열에서 일치하는 항목 검사
     */
    private static boolean containsAnyPattern(String input, String[] patterns) {
        for (String pattern : patterns) {
            if (input.contains(pattern.toLowerCase())) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 비정상적인 반복 패턴 검사 (스팸 방지)
     */
    public static boolean hasExcessiveRepetition(String input) {
        if (input == null || input.length() < 10) return false;
        
        // 1. 동일 문자 연속 반복 검사
        char prevChar = 0;
        int consecutiveCount = 1;
        for (char c : input.toCharArray()) {
            if (c == prevChar) {
                consecutiveCount++;
                if (consecutiveCount > 10) { // 10개 이상 연속
                    return true;
                }
            } else {
                consecutiveCount = 1;
                prevChar = c;
            }
        }
        
        return false;
    }
}