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
				Map.of("role", "system", "content", "너는 친절한 노인 돌봄 비서야."),
				Map.of("role", "user", "content", prompt)
			},
			"max_tokens", 100
		);

		Map<String, Object> response = openAiWebClient.post()
			.uri("/chat/completions")
			.body(BodyInserters.fromValue(requestBody))
			.retrieve()
			.bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
			.block();

		log.info("GPT response: {}", response);

		try {
			return ((Map<String, Object>) ((Map<String, Object>) ((java.util.List<?>) response.get("choices")).get(0)).get("message")).get("content").toString();
		} catch (Exception e) {
			log.error("Failed to parse GPT response", e);
			return "";
		}
	}
}
