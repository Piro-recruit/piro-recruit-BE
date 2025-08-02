package com.pirogramming.recruit.domain.ai_summary.port;

public interface LlmClient {
	String chat(String prompt);
}