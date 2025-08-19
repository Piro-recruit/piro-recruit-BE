package com.pirogramming.recruit.global.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Configuration
public class WebClientConfig {

	@Value("${openai.api.key}")
	private String openAiApiKey;

	@Bean
	public WebClient openAiWebClient() {
		// HTTP 클라이언트 타임아웃 설정
		HttpClient httpClient = HttpClient.create()
			.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000) // 연결 타임아웃: 10초
			.responseTimeout(Duration.ofSeconds(120)) // 응답 타임아웃: 120초 (AI 처리 시간 고려)
			.doOnConnected(conn -> 
				conn.addHandlerLast(new ReadTimeoutHandler(120, TimeUnit.SECONDS)) // 읽기 타임아웃: 120초
					.addHandlerLast(new WriteTimeoutHandler(30, TimeUnit.SECONDS)) // 쓰기 타임아웃: 30초
			);

		return WebClient.builder()
			.baseUrl("https://api.openai.com/v1")
			.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + openAiApiKey)
			.defaultHeader(HttpHeaders.CONTENT_TYPE, "application/json")
			.clientConnector(new ReactorClientHttpConnector(httpClient))
			.codecs(config -> config.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))  // 10MB 까지 허용
			.build();
	}
}