package com.pirogramming.recruit.domain.ai_summary.service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pirogramming.recruit.domain.ai_summary.dto.ApplicationQuestionDto;
import com.pirogramming.recruit.domain.ai_summary.dto.ApplicationSummaryDto;
import com.pirogramming.recruit.domain.ai_summary.exception.AiProcessingException;
import com.pirogramming.recruit.domain.ai_summary.port.LlmClient;
import com.pirogramming.recruit.domain.ai_summary.util.FallbackResponseUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ApplicationProcessingService {
	private final LlmClient llmClient;
	private final ApplicationCacheService cacheService;
	private final ApplicationValidationService validationService;
	private final ObjectMapper objectMapper = new ObjectMapper();

	public ApplicationSummaryDto processApplication(List<ApplicationQuestionDto> questions) {
		try {
			// 1. 캐시된 결과 확인
			ApplicationSummaryDto cachedResult = cacheService.getCachedSummary(questions);
			if (cachedResult != null) {
				return cachedResult;
			}
			
			// 2. 동적 프롬프트 생성
			String prompt = createDynamicSummaryPrompt(questions);
			
			// 3. LLM을 통한 요약 생성
			String llmResponse = llmClient.chat(prompt);
			
			// 4. JSON 응답 파싱
			ApplicationSummaryDto result = parseJsonResponse(llmResponse);
			
			// 5. 결과 캐싱 (유효한 경우에만)
			if (validationService.isValidForCaching(result)) {
				cacheService.cacheSummary(questions, result);
			}
			
			return result;
			
		} catch (AiProcessingException e) {
			log.error("AI processing failed: {} - {}", e.getErrorType(), e.getMessage());
			return createFallbackResponseForError(e.getErrorType());
		} catch (Exception e) {
			log.error("Unexpected error during application processing", e);
			return FallbackResponseUtil.createFallbackSummary("시스템 오류로 인해 처리할 수 없습니다.");
		}
	}
	
	
	/**
	 * 비동기 방식으로 지원서 처리 (성능 개선)
	 */
	public CompletableFuture<ApplicationSummaryDto> processApplicationAsync(List<ApplicationQuestionDto> questions) {
		// 1. 캐시된 결과 확인 (동기)
		ApplicationSummaryDto cachedResult = cacheService.getCachedSummary(questions);
		if (cachedResult != null) {
			return CompletableFuture.completedFuture(cachedResult);
		}
		
		// 2. 동적 프롬프트 생성
		String prompt = createDynamicSummaryPrompt(questions);
		
		// 3. 비동기 LLM 호출
		return llmClient.chatAsync(prompt)
			.thenApply(this::parseJsonResponse)
			.thenApply(result -> {
				// 4. 결과 캐싱 (유효한 경우에만)
				if (validationService.isValidForCaching(result)) {
					cacheService.cacheSummary(questions, result);
				}
				return result;
			})
			.exceptionally(throwable -> {
				if (throwable.getCause() instanceof AiProcessingException) {
					AiProcessingException aiException = (AiProcessingException) throwable.getCause();
					log.error("Async AI processing failed: {} - {}", aiException.getErrorType(), aiException.getMessage());
					return createFallbackResponseForError(aiException.getErrorType());
				} else {
					log.error("Async application processing failed with unexpected error", throwable);
					return FallbackResponseUtil.createFallbackSummary("비동기 처리 중 오류가 발생했습니다.");
				}
			});
	}
	
	/**
	 * 다중 지원서 배치 처리 (병렬 처리)
	 */
	public CompletableFuture<List<ApplicationSummaryDto>> processBatchApplicationsAsync(
			List<List<ApplicationQuestionDto>> applicationsList) {
		
		if (applicationsList == null || applicationsList.isEmpty()) {
			return CompletableFuture.completedFuture(List.of());
		}
		
		// 배치 크기 제한 (DoS 방지)
		if (applicationsList.size() > 10) {
			log.warn("Batch size limited to 10, received: {}", applicationsList.size());
			applicationsList = applicationsList.subList(0, 10);
		}
		
		// 병렬 처리
		List<CompletableFuture<ApplicationSummaryDto>> futures = applicationsList.stream()
			.map(this::processApplicationAsync)
			.collect(Collectors.toList());
		
		// 모든 작업 완료 대기
		return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
			.thenApply(v -> futures.stream()
				.map(CompletableFuture::join)
				.collect(Collectors.toList())
			);
	}
	
	/**
	 * 에러 타입별 맞춤형 fallback 응답 생성
	 */
	private ApplicationSummaryDto createFallbackResponseForError(AiProcessingException.ErrorType errorType) {
		String message = switch (errorType) {
			case NETWORK_ERROR -> "네트워크 연결 문제로 AI 분석을 완료할 수 없습니다. 잠시 후 다시 시도해주세요.";
			case API_LIMIT_EXCEEDED -> "AI API 사용량이 한도를 초과했습니다. 관리자에게 문의하거나 잠시 후 다시 시도해주세요.";
			case INVALID_RESPONSE_FORMAT -> "AI 서비스 응답 형식이 올바르지 않습니다. 수동 검토가 필요합니다.";
			case PARSING_ERROR -> "AI 응답 해석 중 오류가 발생했습니다. 수동 검토가 필요합니다.";
			case VALIDATION_ERROR -> "AI 분석 결과 검증에 실패했습니다. 수동 검토가 필요합니다.";
			case TIMEOUT -> "AI 처리 시간이 초과되었습니다. 지원서 내용을 줄이거나 잠시 후 다시 시도해주세요.";
			case UNKNOWN -> "알 수 없는 오류로 AI 분석을 완료할 수 없습니다. 관리자에게 문의해주세요.";
		};
		
		return FallbackResponseUtil.createFallbackSummary(message);
	}
	
	public ApplicationSummaryDto processApplicationWithDummyData() {
		// 더미 지원서 데이터
		List<ApplicationQuestionDto> dummyQuestions = createDummyQuestions();
		return processApplication(dummyQuestions);
	}
	
	private List<ApplicationQuestionDto> createDummyQuestions() {
		return List.of(
			new ApplicationQuestionDto("지원분야", "백엔드 개발자"),
			new ApplicationQuestionDto("자기소개서", 
				"안녕하세요. 저는 3년차 백엔드 개발자 김철수입니다. " +
				"Java Spring Boot를 주력으로 하며, RESTful API 설계와 구현에 경험이 많습니다. " +
				"최근에는 MSA 아키텍처와 Docker를 활용한 컨테이너화 작업을 진행했습니다."),
			new ApplicationQuestionDto("경력사항",
				"ABC회사 (2022.03 ~ 현재): 백엔드 개발자 - 전자상거래 플랫폼 API 개발, " +
				"일일 트랜잭션 10만건 처리하는 결제 시스템 구축, Redis 캐싱으로 응답속도 40% 개선. " +
				"XYZ스타트업 (2021.01 ~ 2022.02): 주니어 개발자 - 사내 관리자 시스템 개발"),
			new ApplicationQuestionDto("기술스택", "Java, Python, Spring Boot, Django, MySQL, PostgreSQL, Redis, Docker, AWS"),
			new ApplicationQuestionDto("프로젝트 경험",
				"1. 전자상거래 플랫폼: 사용자 10만명 규모 쇼핑몰 백엔드 구축, 검색 속도 60% 향상. " +
				"2. 실시간 채팅 서비스: WebSocket 메시징 시스템, 동시접속자 1000명 처리"),
			new ApplicationQuestionDto("지원동기",
				"귀사의 혁신적인 기술 스택과 성장 가능성에 매력을 느껴 지원했습니다. " +
				"대용량 트래픽 처리와 MSA 환경 경험을 바탕으로 팀의 기술적 성장에 기여하고 싶습니다.")
		);
	}
	
	private String createDynamicSummaryPrompt(List<ApplicationQuestionDto> questions) {
		String questionsText = questions.stream()
			.map(q -> String.format("Q: %s\nA: %s", q.getQuestion(), q.getAnswer()))
			.collect(Collectors.joining("\n\n"));
		
		return """
			Analyze the following university student application for an IT development club recruitment.
			Please respond in Korean language, but follow the English instructions below.
			
			Club Information:
			- Target: University students (both CS majors and non-majors)
			- Type: IT development club focused on learning and collaboration
			- Core Values: Collaboration (협업), Growth (성장), Passion (열정)
			- Looking for: Students who can passionately focus for short periods and collaborate well with others
			
			Application content:
			%s
			
			Please respond in exactly this JSON format:
			{
			  "questionSummaries": [
			    {
			      "question": "original question text",
			      "aiSummary": "Korean summary of this specific question and answer"
			    },
			    // ... repeat for each question
			  ],
			  "scoreOutOf100": evaluation_score_number_between_0_and_100,
			  "scoreReason": "Objective scoring rationale in Korean formal writing style"
			}
			
			For each question-answer pair:
			- Provide a concise Korean summary (1-2 sentences) focusing on key points
			- Highlight relevant skills, experiences, or attitudes shown in that specific answer
			- Keep the original question text exactly as provided
			- Do NOT include the original answer text in the response
			- IMPORTANT: Use formal Korean writing style (문어체) - avoid casual endings like ~습니다, ~입니다. Use ~함, ~됨, ~임 instead
			
			For scoreReason:
			- Provide objective breakdown by evaluation criteria with specific point allocation
			- Format: "열정 및 학습 태도: X/40점 - specific evidence. 협업 잠재력: Y/30점 - specific evidence. 기술적 기반: Z/20점 - specific evidence. 성장 마인드셋: W/10점 - specific evidence."
			- Base points on concrete evidence from the answers, not subjective impressions
			- IMPORTANT: Use formal Korean writing style (문어체) - avoid ~습니다, ~입니다 endings. Use ~함, ~됨, ~임, ~보임 instead
			
			Evaluation Criteria (100 points total):
			1. Passion & Learning Attitude (40 points):
			   - Enthusiasm for IT development and learning
			   - Self-learning ability and curiosity
			   - Willingness to challenge new technologies
			   
			2. Collaboration Potential (30 points):
			   - Communication skills and teamwork experience
			   - Openness to feedback and different perspectives
			   - Leadership or mentoring experience
			   
			3. Technical Foundation (20 points):
			   - Programming languages, frameworks, tools knowledge
			   - Project experience (personal, academic, or team projects)
			   - Problem-solving approach
			   
			4. Growth Mindset (10 points):
			   - Willingness to learn from failures
			   - Goal-setting and improvement orientation
			   - Adaptability to club activities and short-term intensive projects
			
			Scoring Guidelines:
			- 90-100: Exceptional candidate with strong passion, collaboration skills, and technical foundation
			- 80-89: Very good candidate with most qualities aligned with club values
			- 70-79: Good candidate with solid foundation but some areas for development
			- 60-69: Adequate candidate with basic qualifications
			- 50-59: Below average candidate with limited alignment
			- 0-49: Poor fit for the club
			
			Important notes:
			- Consider both CS majors and non-majors fairly
			- Value learning potential over current technical level for beginners
			- Emphasize collaboration and passion over pure technical skills
			- All text values must be written in Korean using formal writing style (문어체)
			- Avoid conversational endings (~습니다, ~입니다) and use formal endings (~함, ~됨, ~임, ~보임)
			- Return only valid JSON without any additional text or markdown formatting
			""".formatted(questionsText);
	}
	
	private ApplicationSummaryDto parseJsonResponse(String jsonResponse) {
		try {
			// 입력 검증
			if (jsonResponse == null || jsonResponse.trim().isEmpty()) {
				log.warn("Empty JSON response received from AI service");
				throw new AiProcessingException(AiProcessingException.ErrorType.INVALID_RESPONSE_FORMAT, 
					"AI 서비스로부터 빈 응답을 받았습니다.");
			}
			
			// 길이 제한 (DoS 방지)
			if (jsonResponse.length() > 50000) {
				log.warn("JSON response too large: {} characters", jsonResponse.length());
				throw new AiProcessingException(AiProcessingException.ErrorType.INVALID_RESPONSE_FORMAT, 
					"AI 응답이 너무 큽니다.");
			}
			
			// JSON 응답에서 실제 JSON 부분만 안전하게 추출
			String cleanJson = extractJsonFromResponseSafely(jsonResponse);
			ApplicationSummaryDto result = objectMapper.readValue(cleanJson, ApplicationSummaryDto.class);
			
			// 파싱된 결과 검증
			return validationService.validateAndSanitizeSummary(result);
			
		} catch (AiProcessingException e) {
			// 이미 분류된 AI 처리 예외는 그대로 재던지기
			throw e;
		} catch (JsonProcessingException e) {
			log.error("JSON parsing failed for AI response", e);
			throw new AiProcessingException(AiProcessingException.ErrorType.PARSING_ERROR, 
				"AI 응답 JSON 파싱에 실패했습니다.", e);
		} catch (IllegalArgumentException e) {
			log.error("Invalid argument in JSON processing", e);
			throw new AiProcessingException(AiProcessingException.ErrorType.VALIDATION_ERROR, 
				"AI 응답 형식이 올바르지 않습니다.", e);
		} catch (Exception e) {
			log.error("Unexpected error during JSON response processing", e);
			throw new AiProcessingException(AiProcessingException.ErrorType.UNKNOWN, 
				"AI 응답 처리 중 예상치 못한 오류가 발생했습니다.", e);
		}
	}
	
	
	
	/**
	 * 안전한 JSON 추출 (보안 강화)
	 */
	private String extractJsonFromResponseSafely(String response) {
		if (response == null || response.trim().isEmpty()) {
			throw new AiProcessingException(AiProcessingException.ErrorType.INVALID_RESPONSE_FORMAT, 
				"AI 서비스 응답이 비어있습니다.");
		}
		
		// 중괄호 쌍을 정확히 매칭하여 JSON 추출
		int braceCount = 0;
		int startIndex = -1;
		int endIndex = -1;
		
		for (int i = 0; i < response.length(); i++) {
			char c = response.charAt(i);
			if (c == '{') {
				if (startIndex == -1) {
					startIndex = i;
				}
				braceCount++;
			} else if (c == '}') {
				braceCount--;
				if (braceCount == 0 && startIndex != -1) {
					endIndex = i;
					break;
				}
			}
		}
		
		if (startIndex == -1 || endIndex == -1 || endIndex <= startIndex) {
			throw new AiProcessingException(AiProcessingException.ErrorType.INVALID_RESPONSE_FORMAT, 
				"AI 응답에서 유효한 JSON 구조를 찾을 수 없습니다.");
		}
		
		String jsonCandidate = response.substring(startIndex, endIndex + 1);
		
		// 기본적인 JSON 구조 검증
		if (!jsonCandidate.contains("questionSummaries") || 
			!jsonCandidate.contains("scoreOutOf100") ||
			!jsonCandidate.contains("scoreReason")) {
			throw new AiProcessingException(AiProcessingException.ErrorType.INVALID_RESPONSE_FORMAT, 
				"AI 응답에 필수 필드가 누락되었습니다.");
		}
		
		return jsonCandidate;
	}
}
