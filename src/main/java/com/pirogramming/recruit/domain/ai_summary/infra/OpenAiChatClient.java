package com.pirogramming.recruit.domain.ai_summary.infra;

import java.util.Map;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.pirogramming.recruit.domain.ai_summary.port.LlmClient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class OpenAiChatClient implements LlmClient {

	private final WebClient openAiWebClient;

	@Override
	public String chat(String prompt) {
		Map<String, Object> requestBody = Map.of(
			"model", "gpt-4o",
			"messages", new Object[]{
				Map.of("role", "system", "content", 
					"You are a professional HR assistant specialized in analyzing job applications. " +
					"Provide objective, detailed analysis focusing on technical skills, experience, and qualifications. " +
					"Always respond in Korean language as instructed in the user prompt."),
				Map.of("role", "user", "content", prompt)
			},
			"max_tokens", 1500,
			"temperature", 0.3,
			"response_format", Map.of("type", "json_object")
		);

		try {
			Map<String, Object> response = openAiWebClient.post()
				.uri("/chat/completions")
				.body(BodyInserters.fromValue(requestBody))
				.retrieve()
				.bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
				.block();

			log.info("OpenAI API call completed successfully");
			// 보안: 응답 내용은 debug 레벨에서도 로깅하지 않음 (민감정보 보호)

			return extractContentFromResponse(response);
		} catch (WebClientResponseException e) {
			// HTTP 에러 상세 로깅 (민감정보 제외)
			log.error("OpenAI API HTTP error - Status: {}, Error: {}", 
				e.getStatusCode(), 
				sanitizeErrorMessage(e.getResponseBodyAsString()));
			return createFallbackResponse();
		} catch (Exception e) {
			// 타임아웃 포함 일반적인 예외 처리
			log.error("OpenAI API call failed: {}", e.getClass().getSimpleName());
			return createFallbackResponse();
		}
	}
	
	private String extractContentFromResponse(Map<String, Object> response) {
		try {
			// 입력 검증
			if (response == null || response.isEmpty()) {
				throw new RuntimeException("Empty response from OpenAI");
			}
			
			@SuppressWarnings("unchecked")
			var choices = (java.util.List<Map<String, Object>>) response.get("choices");
			if (choices == null || choices.isEmpty()) {
				throw new RuntimeException("No choices in response");
			}
			
			@SuppressWarnings("unchecked")
			var message = (Map<String, Object>) choices.get(0).get("message");
			if (message == null) {
				throw new RuntimeException("No message in choice");
			}
			
			var content = message.get("content");
			if (content == null) {
				throw new RuntimeException("No content in message");
			}
			
			String contentStr = content.toString();
			
			// 콘텐츠 내용 기본 검증
			if (contentStr.trim().isEmpty()) {
				throw new RuntimeException("Empty content in response");
			}
			
			// 응답 길이 제한 (DoS 방지)
			if (contentStr.length() > 10000) {
				log.warn("OpenAI response too long, truncating: {} chars", contentStr.length());
				contentStr = contentStr.substring(0, 10000);
			}
			
			return contentStr;
			
		} catch (ClassCastException e) {
			log.error("Invalid response structure from OpenAI: type mismatch");
			throw new RuntimeException("Invalid response structure from OpenAI", e);
		} catch (Exception e) {
			log.error("Failed to parse OpenAI response: {}", e.getClass().getSimpleName());
			throw new RuntimeException("Invalid response structure from OpenAI", e);
		}
	}
	
	/**
	 * 안전한 에러 메시지 정제 (민감정보 제거)
	 */
	private String sanitizeErrorMessage(String errorMessage) {
		if (errorMessage == null || errorMessage.isEmpty()) {
			return "No error details available";
		}
		
		// API 키, 토큰 등 민감정보 제거
		String sanitized = errorMessage
			.replaceAll("Bearer [A-Za-z0-9\\-_.]+", "Bearer [REDACTED]")
			.replaceAll("sk-[A-Za-z0-9]+", "[API_KEY_REDACTED]")
			.replaceAll("Authorization: .*", "Authorization: [REDACTED]")
			.replaceAll("token[\\s:=]+[A-Za-z0-9\\-_.]+", "token=[REDACTED]");
		
		// 길이 제한 (500자)
		if (sanitized.length() > 500) {
			sanitized = sanitized.substring(0, 497) + "...";
		}
		
		return sanitized;
	}
	
	private String createFallbackResponse() {
		return """
			{
			  "questionSummaries": [],
			  "scoreOutOf100": 0,
			  "scoreReason": "AI 분석 서비스 오류로 인해 평가를 완료할 수 없습니다. 수동 검토가 필요합니다."
			}
			""";
	}
}
