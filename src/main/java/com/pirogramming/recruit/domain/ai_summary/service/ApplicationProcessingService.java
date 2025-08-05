package com.pirogramming.recruit.domain.ai_summary.service;

import java.util.List;
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
			
			Please respond in exactly this JSON format (all values must be in Korean):
			{
			  "overallSummary": "Overall summary of the applicant (2-3 sentences in Korean)",
			  "keyStrengths": ["Key strength 1 in Korean", "Key strength 2 in Korean", "Key strength 3 in Korean"],
			  "technicalSkills": ["Technical skill 1", "Technical skill 2", "Technical skill 3"],
			  "experience": "Experience summary including projects, studies, self-learning (1-2 sentences in Korean)",
			  "motivation": "Motivation summary (1 sentence in Korean)",
			  "scoreOutOf100": evaluation_score_number_between_0_and_100
			}
			
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
			// JSON 응답에서 실제 JSON 부분만 추출
			String cleanJson = extractJsonFromResponse(jsonResponse);
			return objectMapper.readValue(cleanJson, ApplicationSummaryDto.class);
		} catch (JsonProcessingException e) {
			// JSON 파싱 실패 시 기본값 반환
			return new ApplicationSummaryDto(
				"JSON 파싱 오류로 인한 기본 요약",
				List.of("파싱 오류"),
				List.of("N/A"),
				"경력 정보 파싱 실패",
				"동기 파싱 실패",
				50
			);
		}
	}
	
	private String extractJsonFromResponse(String response) {
		// JSON 블록 찾기 (```json 또는 { 로 시작)
		int startIndex = response.indexOf("{");
		int endIndex = response.lastIndexOf("}");
		
		if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
			return response.substring(startIndex, endIndex + 1);
		}
		
		throw new IllegalArgumentException("유효한 JSON을 찾을 수 없습니다: " + response);
	}
}
