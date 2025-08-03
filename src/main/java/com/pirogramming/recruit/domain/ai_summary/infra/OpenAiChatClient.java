package com.pirogramming.recruit.domain.ai_summary.infra;

import java.util.Map;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

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

			log.info("GPT response received successfully");
			log.debug("GPT response details: {}", response);

			return extractContentFromResponse(response);
		} catch (Exception e) {
			log.error("Failed to call OpenAI API", e);
			return createFallbackResponse();
		}
	}
	
	private String extractContentFromResponse(Map<String, Object> response) {
		try {
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
			
			return content.toString();
		} catch (Exception e) {
			log.error("Failed to parse GPT response structure", e);
			throw new RuntimeException("Invalid response structure from OpenAI", e);
		}
	}
	
	private String createFallbackResponse() {
		return """
			{
			  "overallSummary": "OpenAI API 호출 중 오류가 발생하여 자동 분석을 완료할 수 없습니다.",
			  "keyStrengths": ["API 오류", "수동 검토 필요"],
			  "technicalSkills": ["정보 없음"],
			  "experience": "OpenAI 서비스 오류로 인해 경험 분석을 완료할 수 없습니다.",
			  "motivation": "수동으로 검토해주세요.",
			  "scoreOutOf100": 0
			}
			""";
	}
}
