package com.pirogramming.recruit.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

	@Value("${openai.api.key}")
	private String openAiApiKey;

	@Bean
	public WebClient openAiWebClient() {
		return WebClient.builder()
			.baseUrl("https://api.openai.com/v1")
			.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + openAiApiKey)
			.codecs(config -> config.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))  // 10MB 까지 허용
			.build();
	}
}
