package com.pirogramming.recruit.domain.ai_summary.exception;

/**
 * AI 처리 관련 예외
 */
public class AiProcessingException extends RuntimeException {
    
    private final ErrorType errorType;
    
    public AiProcessingException(ErrorType errorType, String message) {
        super(message);
        this.errorType = errorType;
    }
    
    public AiProcessingException(ErrorType errorType, String message, Throwable cause) {
        super(message, cause);
        this.errorType = errorType;
    }
    
    public ErrorType getErrorType() {
        return errorType;
    }
    
    public enum ErrorType {
        NETWORK_ERROR("네트워크 오류"),
        API_LIMIT_EXCEEDED("API 한도 초과"),
        INVALID_RESPONSE_FORMAT("응답 형식 오류"),
        PARSING_ERROR("응답 파싱 오류"),
        VALIDATION_ERROR("응답 검증 오류"),
        TIMEOUT("처리 시간 초과"),
        UNKNOWN("알 수 없는 오류");
        
        private final String description;
        
        ErrorType(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
}