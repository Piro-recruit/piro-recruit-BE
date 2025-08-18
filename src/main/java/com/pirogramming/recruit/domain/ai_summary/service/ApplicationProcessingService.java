package com.pirogramming.recruit.domain.ai_summary.service;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pirogramming.recruit.domain.ai_summary.dto.ApplicationQuestionDto;
import com.pirogramming.recruit.domain.ai_summary.dto.ApplicationSummaryDto;
import com.pirogramming.recruit.domain.ai_summary.port.LlmClient;
import com.pirogramming.recruit.domain.ai_summary.util.FallbackResponseUtil;
import com.pirogramming.recruit.domain.ai_summary.util.TextSanitizerUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ApplicationProcessingService {
	private final LlmClient llmClient;
	private final ApplicationCacheService cacheService;
	private final ObjectMapper objectMapper = new ObjectMapper();

	public ApplicationSummaryDto processApplication(List<ApplicationQuestionDto> questions) {
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
		if (isValidForCaching(result)) {
			cacheService.cacheSummary(questions, result);
		}
		
		return result;
	}
	
	/**
	 * 캐싱 가능한 유효한 결과인지 검사
	 */
	private boolean isValidForCaching(ApplicationSummaryDto result) {
		if (result == null) return false;
		
		// 폴백 응답은 캐싱하지 않음
		if (result.getScoreOutOf100() == 0 && 
			result.getScoreReason().contains("오류")) {
			return false;
		}
		
		// 빈 요약은 캐싱하지 않음
		if (result.getQuestionSummaries() == null || 
			result.getQuestionSummaries().isEmpty()) {
			return false;
		}
		
		return true;
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
				if (isValidForCaching(result)) {
					cacheService.cacheSummary(questions, result);
				}
				return result;
			})
			.exceptionally(throwable -> {
				log.error("Async application processing failed", throwable);
				return FallbackResponseUtil.createFallbackSummary();
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
			- Use formal Korean writing style (문어체) for all summaries
			
			For scoreReason:
			- Provide objective breakdown by evaluation criteria with specific point allocation
			- Format: "열정 및 학습 태도: X/40점 - specific evidence. 협업 잠재력: Y/30점 - specific evidence. 기술적 기반: Z/20점 - specific evidence. 성장 마인드셋: W/10점 - specific evidence."
			- Base points on concrete evidence from the answers, not subjective impressions
			- Use formal Korean writing style (문어체)
			
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
			- All text values must be written in Korean
			- Return only valid JSON without any additional text or markdown formatting
			""".formatted(questionsText);
	}
	
	private ApplicationSummaryDto parseJsonResponse(String jsonResponse) {
		try {
			// 입력 검증
			if (jsonResponse == null || jsonResponse.trim().isEmpty()) {
				return FallbackResponseUtil.createFallbackSummary();
			}
			
			// 길이 제한 (DoS 방지)
			if (jsonResponse.length() > 50000) {
				return FallbackResponseUtil.createFallbackSummary();
			}
			
			// JSON 응답에서 실제 JSON 부분만 안전하게 추출
			String cleanJson = extractJsonFromResponseSafely(jsonResponse);
			ApplicationSummaryDto result = objectMapper.readValue(cleanJson, ApplicationSummaryDto.class);
			
			// 파싱된 결과 검증
			return validateAndSanitizeSummary(result);
			
		} catch (JsonProcessingException e) {
			return FallbackResponseUtil.createFallbackSummary();
		} catch (IllegalArgumentException e) {
			return FallbackResponseUtil.createFallbackSummary();
		} catch (Exception e) {
			// 예상치 못한 오류는 민감정보 로깅 방지
			return FallbackResponseUtil.createFallbackSummary();
		}
	}
	
	
	/**
	 * 파싱된 요약 결과 검증 및 정제 (강화된 버전)
	 */
	private ApplicationSummaryDto validateAndSanitizeSummary(ApplicationSummaryDto summary) {
		if (summary == null) {
			return FallbackResponseUtil.createFallbackSummary();
		}
		
		// 1. 점수 범위 및 일관성 검증
		int score = validateScore(summary.getScoreOutOf100());
		
		// 2. 질문별 요약 검증 및 정제
		List<ApplicationSummaryDto.QuestionSummaryDto> cleanQuestionSummaries = 
			validateAndCleanQuestionSummaries(summary.getQuestionSummaries());
		
		// 3. scoreReason 검증 및 정제
		String cleanScoreReason = validateAndCleanScoreReason(summary.getScoreReason(), score);
		
		// 4. 전체 내용 일관성 검사
		if (!isContentConsistent(cleanQuestionSummaries, score, cleanScoreReason)) {
			log.warn("Inconsistent AI response detected, applying corrections");
			score = adjustScoreForInconsistency(score);
		}
		
		ApplicationSummaryDto result = new ApplicationSummaryDto();
		result.setQuestionSummaries(cleanQuestionSummaries);
		result.setScoreOutOf100(score);
		result.setScoreReason(cleanScoreReason);
		return result;
	}
	
	/**
	 * 점수 유효성 검증
	 */
	private int validateScore(int score) {
		// 기본 범위 검증
		if (score < 0 || score > 100) {
			score = Math.max(0, Math.min(100, score));
		}
		
		// 비현실적 점수 검사 (너무 극단적인 점수 조정)
		if (score == 0 || score == 100) {
			log.info("Extreme score detected: {}, applying moderation", score);
			score = score == 0 ? 10 : 95; // 예외상황 제외하고 극단적 점수 완화
		}
		
		return score;
	}
	
	/**
	 * 질문별 요약 검증 및 정제
	 */
	private List<ApplicationSummaryDto.QuestionSummaryDto> validateAndCleanQuestionSummaries(
			List<ApplicationSummaryDto.QuestionSummaryDto> questionSummaries) {
		
		if (questionSummaries == null || questionSummaries.isEmpty()) {
			return List.of();
		}
		
		return questionSummaries.stream()
			.filter(Objects::nonNull)
			.map(this::sanitizeQuestionSummary)
			.filter(Objects::nonNull) // 정제 실패 제외
			.filter(q -> !q.getAiSummary().trim().isEmpty()) // 빈 요약 제외
			.limit(30) // DoS 방지
			.collect(Collectors.toList());
	}
	
	/**
	 * 점수 근거 검증 및 정제
	 */
	private String validateAndCleanScoreReason(String scoreReason, int score) {
		String cleaned = TextSanitizerUtil.sanitize(scoreReason);
		
		if (cleaned == null || cleaned.trim().isEmpty()) {
			return "점수 근거를 제공할 수 없습니다.";
		}
		
		// 점수 근거의 기본 구조 검증
		if (!isValidScoreReasonFormat(cleaned)) {
			log.warn("Invalid score reason format detected");
			return "점수 근거 형식이 올바르지 않아 자동 검토가 필요합니다.";
		}
		
		return cleaned;
	}
	
	/**
	 * 점수 근거 형식 유효성 검사
	 */
	private boolean isValidScoreReasonFormat(String scoreReason) {
		// 기본적인 평가 영역들이 포함되어 있는지 검사
		String[] expectedKeywords = {"열정", "협업", "기술", "성장"};
		String lowerReason = scoreReason.toLowerCase();
		
		int foundKeywords = 0;
		for (String keyword : expectedKeywords) {
			if (lowerReason.contains(keyword)) {
				foundKeywords++;
			}
		}
		
		// 최소 2개 이상의 평가 영역이 언급되어야 함
		return foundKeywords >= 2;
	}
	
	/**
	 * 전체 내용 일관성 검사
	 */
	private boolean isContentConsistent(List<ApplicationSummaryDto.QuestionSummaryDto> summaries, 
									   int score, String scoreReason) {
		// 1. 요약 개수와 점수의 일관성
		if (summaries.isEmpty() && score > 20) {
			return false; // 요약이 없는데 높은 점수
		}
		
		// 2. 점수와 근거의 일관성 (단순 검사)
		if (score >= 80 && scoreReason.contains("부족")) {
			return false; // 높은 점수인데 부족하다는 평가
		}
		
		if (score <= 30 && scoreReason.contains("우수")) {
			return false; // 낮은 점수인데 우수하다는 평가
		}
		
		return true;
	}
	
	/**
	 * 비일관성 발견 시 점수 조정
	 */
	private int adjustScoreForInconsistency(int originalScore) {
		// 비일관성이 발견된 경우 중간 점수대로 조정
		if (originalScore >= 80) {
			return 70; // 높은 점수는 중간으로
		} else if (originalScore <= 30) {
			return 40; // 낮은 점수도 중간으로
		}
		return originalScore; // 중간 점수는 유지
	}
	
	/**
	 * 개별 질문 요약 정제 (강화된 버전)
	 */
	private ApplicationSummaryDto.QuestionSummaryDto sanitizeQuestionSummary(ApplicationSummaryDto.QuestionSummaryDto questionSummary) {
		if (questionSummary == null) {
			return null;
		}
		
		String cleanQuestion = TextSanitizerUtil.sanitize(questionSummary.getQuestion());
		String cleanAiSummary = TextSanitizerUtil.sanitize(questionSummary.getAiSummary());
		
		// 빈 내용 검사
		if (cleanQuestion.trim().isEmpty() || cleanAiSummary.trim().isEmpty()) {
			return null;
		}
		
		// AI 요약이 원본 답변을 그대로 복사한 경우 감지
		if (cleanAiSummary.length() > 500 && containsOriginalAnswer(cleanAiSummary)) {
			log.warn("AI summary appears to contain original answer, truncating");
			cleanAiSummary = truncateToSummary(cleanAiSummary);
		}
		
		// 부적절한 내용 감지
		if (containsInappropriateContent(cleanAiSummary)) {
			log.warn("Inappropriate content detected in AI summary");
			cleanAiSummary = "요약 내용에 부적절한 내용이 감지되어 수동 검토가 필요합니다.";
		}
		
		return new ApplicationSummaryDto.QuestionSummaryDto(cleanQuestion, cleanAiSummary);
	}
	
	/**
	 * 원본 답변 포함 여부 감지
	 */
	private boolean containsOriginalAnswer(String aiSummary) {
		// 요약이 아닌 원본 답변을 그대로 복사한 경우를 감지
		// 이는 간단한 휴리스틱으로 정확하지 않을 수 있음
		
		// 1. 반복적인 문장 구조 감지
		String[] longSentenceIndicators = {
			"저는", "제가", "바랍니다", "생각합니다", "경험이 있습니다"
		};
		
		int indicatorCount = 0;
		for (String indicator : longSentenceIndicators) {
			if (aiSummary.contains(indicator)) {
				indicatorCount++;
			}
		}
		
		return indicatorCount >= 3; // 3개 이상의 지시어가 있으면 원본 답변일 가능성
	}
	
	/**
	 * 요약으로 잘라내기
	 */
	private String truncateToSummary(String text) {
		// 첫 번째 문장만 추출하여 요약으로 사용
		String[] sentences = text.split("[.!?]");
		if (sentences.length > 0) {
			String firstSentence = sentences[0].trim();
			if (firstSentence.length() > 100) {
				return firstSentence.substring(0, 97) + "...";
			}
			return firstSentence + ".";
		}
		return "요약 생성 실패.";
	}
	
	/**
	 * 부적절한 내용 감지
	 */
	private boolean containsInappropriateContent(String content) {
		// 기본적인 부적절 내용 감지
		String[] inappropriatePatterns = {
			"개인정보", "전화번호", "주소", "이메일", 
			"비밀번호", "주민등록번호", "password", "email"
		};
		
		String lowerContent = content.toLowerCase();
		for (String pattern : inappropriatePatterns) {
			if (lowerContent.contains(pattern.toLowerCase())) {
				return true;
			}
		}
		
		return false;
	}
	
	
	/**
	 * 안전한 JSON 추출 (보안 강화)
	 */
	private String extractJsonFromResponseSafely(String response) {
		if (response == null || response.trim().isEmpty()) {
			throw new IllegalArgumentException("응답이 비어있습니다");
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
			throw new IllegalArgumentException("유효한 JSON 구조를 찾을 수 없습니다");
		}
		
		String jsonCandidate = response.substring(startIndex, endIndex + 1);
		
		// 기본적인 JSON 구조 검증
		if (!jsonCandidate.contains("questionSummaries") || 
			!jsonCandidate.contains("scoreOutOf100") ||
			!jsonCandidate.contains("scoreReason")) {
			throw new IllegalArgumentException("필수 필드가 누락된 JSON입니다");
		}
		
		return jsonCandidate;
	}
}
