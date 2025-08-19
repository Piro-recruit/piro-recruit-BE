package com.pirogramming.recruit.domain.ai_summary.port;

import java.util.concurrent.CompletableFuture;

public interface LlmClient {
	/**
	 * 동기 방식 LLM 호출 (기존 호환성)
	 */
	String chat(String prompt);
	
	/**
	 * 비동기 방식 LLM 호출 (성능 개선)
	 */
	CompletableFuture<String> chatAsync(String prompt);
}