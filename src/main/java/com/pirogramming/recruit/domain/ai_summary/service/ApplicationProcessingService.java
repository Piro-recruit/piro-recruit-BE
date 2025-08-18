package com.pirogramming.recruit.domain.ai_summary.service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pirogramming.recruit.domain.ai_summary.dto.ApplicationQuestionDto;
import com.pirogramming.recruit.domain.ai_summary.dto.ApplicationSummaryDto;
import com.pirogramming.recruit.domain.ai_summary.port.LlmClient;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class ApplicationProcessingService {
	private final LlmClient llmClient;
	private final ObjectMapper objectMapper = new ObjectMapper();

	public ApplicationSummaryDto processApplication(List<ApplicationQuestionDto> questions) {
		// 동적 프롬프트 생성
		String prompt = createDynamicSummaryPrompt(questions);
		
		// LLM을 통한 요약 생성
		String llmResponse = llmClient.chat(prompt);
		
		// JSON 응답 파싱
		return parseJsonResponse(llmResponse);
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
			  "scoreOutOf100": evaluation_score_number_between_0_and_100
			}
			
			For each question-answer pair:
			- Provide a concise Korean summary (1-2 sentences) focusing on key points
			- Highlight relevant skills, experiences, or attitudes shown in that specific answer
			- Keep the original question text exactly as provided
			- Do NOT include the original answer text in the response
			
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
				return createFallbackSummary();
			}
			
			// 길이 제한 (DoS 방지)
			if (jsonResponse.length() > 50000) {
				return createFallbackSummary();
			}
			
			// JSON 응답에서 실제 JSON 부분만 안전하게 추출
			String cleanJson = extractJsonFromResponseSafely(jsonResponse);
			ApplicationSummaryDto result = objectMapper.readValue(cleanJson, ApplicationSummaryDto.class);
			
			// 파싱된 결과 검증
			return validateAndSanitizeSummary(result);
			
		} catch (JsonProcessingException e) {
			return createFallbackSummary();
		} catch (IllegalArgumentException e) {
			return createFallbackSummary();
		} catch (Exception e) {
			// 예상치 못한 오류는 민감정보 로깅 방지
			return createFallbackSummary();
		}
	}
	
	/**
	 * 폴백 요약 생성 (민감정보 로깅 방지)
	 */
	private ApplicationSummaryDto createFallbackSummary() {
		ApplicationSummaryDto fallback = new ApplicationSummaryDto();
		fallback.setQuestionSummaries(List.of());
		fallback.setScoreOutOf100(0);
		return fallback;
	}
	
	/**
	 * 파싱된 요약 결과 검증 및 정제
	 */
	private ApplicationSummaryDto validateAndSanitizeSummary(ApplicationSummaryDto summary) {
		if (summary == null) {
			return createFallbackSummary();
		}
		
		// 점수 범위 검증
		int score = summary.getScoreOutOf100();
		if (score < 0 || score > 100) {
			score = Math.max(0, Math.min(100, score));
		}
		
		// 질문별 요약 검증 및 정제
		List<ApplicationSummaryDto.QuestionSummaryDto> cleanQuestionSummaries = null;
		if (summary.getQuestionSummaries() != null) {
			cleanQuestionSummaries = summary.getQuestionSummaries().stream()
				.filter(Objects::nonNull)
				.map(this::sanitizeQuestionSummary)
				.collect(Collectors.toList());
		}
		
		ApplicationSummaryDto result = new ApplicationSummaryDto();
		result.setQuestionSummaries(cleanQuestionSummaries != null ? cleanQuestionSummaries : List.of());
		result.setScoreOutOf100(score);
		return result;
	}
	
	/**
	 * 개별 질문 요약 정제
	 */
	private ApplicationSummaryDto.QuestionSummaryDto sanitizeQuestionSummary(ApplicationSummaryDto.QuestionSummaryDto questionSummary) {
		if (questionSummary == null) {
			return null;
		}
		
		String cleanQuestion = sanitizeText(questionSummary.getQuestion());
		String cleanAiSummary = sanitizeText(questionSummary.getAiSummary());
		
		return new ApplicationSummaryDto.QuestionSummaryDto(
			cleanQuestion,
			cleanAiSummary
		);
	}
	
	/**
	 * 텍스트 정제 (길이 제한 및 유해 콘텐츠 제거)
	 */
	private String sanitizeText(String text) {
		if (text == null) return "";
		
		String cleaned = text
			.replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", "") // 제어 문자 제거
			.replaceAll("\\s+", " ") // 연속 공백 정규화
			.trim();
		
		// 길이 제한
		if (cleaned.length() > 1000) {
			cleaned = cleaned.substring(0, 997) + "...";
		}
		
		return cleaned;
	}
	
	/**
	 * 리스트 정제
	 */
	private List<String> sanitizeList(List<String> list) {
		if (list == null) return List.of();
		
		return list.stream()
			.filter(Objects::nonNull)
			.map(this::sanitizeText)
			.filter(s -> !s.trim().isEmpty())
			.collect(Collectors.toList());
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
			!jsonCandidate.contains("scoreOutOf100")) {
			throw new IllegalArgumentException("필수 필드가 누락된 JSON입니다");
		}
		
		return jsonCandidate;
	}
}
