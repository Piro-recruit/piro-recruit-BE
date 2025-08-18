package com.pirogramming.recruit.domain.ai_summary.controller;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pirogramming.recruit.domain.ai_summary.dto.ApplicationQuestionDto;
import com.pirogramming.recruit.domain.ai_summary.dto.ApplicationSummaryDto;
import com.pirogramming.recruit.domain.ai_summary.service.ApplicationProcessingService;
import com.pirogramming.recruit.global.exception.ApiRes;
import com.pirogramming.recruit.global.exception.code.ErrorCode;
import com.pirogramming.recruit.global.security.RequireAdmin;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Tag(name = "AI 지원서 요약", description = "대학생 IT 개발 동아리 지원서 AI 자동 분석 및 평가 API")
@Slf4j
@RestController
@RequestMapping("/api/ai-summary")
@RequiredArgsConstructor
public class AiSummaryController {
	
	private final ApplicationProcessingService applicationProcessingService;
	
	@Operation(
		summary = "더미 지원서 분석 테스트",
		description = "미리 준비된 더미 지원서 데이터를 사용하여 IT 개발 동아리 맞춤 AI 분석 기능을 테스트합니다."
	)
	@ApiResponses(value = {
		@ApiResponse(
			responseCode = "200",
			description = "요약 성공",
			content = @Content(
				mediaType = "application/json",
				schema = @Schema(implementation = ApiRes.class),
				examples = @ExampleObject(
					name = "성공 응답 예시",
					value = """
						{
						  "success": true,
						  "data": {
						    "overallSummary": "컴퓨터공학과 3학년으로 Spring Boot 기반 프로젝트 경험이 있으며, 팀 프로젝트를 통해 협업 능력을 보여주었습니다.",
						    "keyStrengths": ["팀 프로젝트 리더십", "새로운 기술 학습 의욕", "문제 해결 능력"],
						    "technicalSkills": ["Java", "Spring Boot", "JavaScript", "MySQL"],
						    "experience": "학교 수업과 개인 프로젝트를 통해 웹 개발 기초를 익혔으며, 팀 프로젝트에서 백엔드 개발을 담당했습니다.",
						    "motivation": "동아리 활동을 통해 실무 경험을 쌓고 다른 학생들과 함께 성장하고 싶습니다.",
						    "scoreOutOf100": 78
						  },
						  "message": "지원서 요약이 성공적으로 생성되었습니다.",
						  "status": 200
						}
						"""
				)
			)
		),
		@ApiResponse(
			responseCode = "500",
			description = "AI 서비스 오류",
			content = @Content(
				mediaType = "application/json",
				examples = @ExampleObject(
					name = "API 오류 시 fallback 응답",
					value = """
						{
						  "success": true,
						  "data": {
						    "overallSummary": "OpenAI API 호출 중 오류가 발생하여 자동 분석을 완료할 수 없습니다.",
						    "keyStrengths": ["API 오류", "수동 검토 필요"],
						    "technicalSkills": ["정보 없음"],
						    "experience": "OpenAI 서비스 오류로 인해 경험 분석을 완료할 수 없습니다.",
						    "motivation": "수동으로 검토해주세요.",
						    "scoreOutOf100": 0
						  },
						  "message": "지원서 요약이 성공적으로 생성되었습니다.",
						  "status": 200
						}
						"""
				)
			)
		)
	})
	@GetMapping("/test")
	@RequireAdmin
	public ApiRes<ApplicationSummaryDto> testSummary() {
		ApplicationSummaryDto summary = applicationProcessingService.processApplicationWithDummyData();
		return ApiRes.success(summary, "지원서 요약이 성공적으로 생성되었습니다.");
	}
	
	@Operation(
		summary = "동적 지원서 분석",
		description = "대학생 IT 개발 동아리 지원서를 AI로 분석하여 협업, 성장, 열정 기준으로 평가합니다. 전공/비전공 구분 없이 학습 의지와 협업 능력을 중점 평가합니다."
	)
	@ApiResponses(value = {
		@ApiResponse(
			responseCode = "200",
			description = "분석 성공",
			content = @Content(
				mediaType = "application/json",
				schema = @Schema(implementation = ApiRes.class)
			)
		),
		@ApiResponse(
			responseCode = "400",
			description = "잘못된 요청 형식",
			content = @Content(
				mediaType = "application/json",
				examples = @ExampleObject(
					name = "잘못된 요청 예시",
					value = """
						{
						  "success": false,
						  "data": null,
						  "message": "잘못된 요청 형식입니다.",
						  "status": 400
						}
						"""
				)
			)
		)
	})
	@PostMapping("/analyze")
	@RequireAdmin
	public ApiRes<ApplicationSummaryDto> analyzeDynamicApplication(
		@io.swagger.v3.oas.annotations.parameters.RequestBody(
			description = "지원서 문항과 답변 목록 (최대 20개)",
			required = true,
			content = @Content(
				mediaType = "application/json",
				schema = @Schema(implementation = ApplicationQuestionDto.class),
				examples = {
					@ExampleObject(
						name = "실제 지원서 예시",
						value = """
							[
							  {
							    "question": "1. 본인의 가치관, 성격 등을 포함한 자기소개와 피로그래밍에 지원한 동기 및 목표를 적어주세요",
							    "answer": "안녕하세요. 컴퓨터공학을 전공하고 있는 학생입니다. 웹 개발에 관심이 많아 피로그래밍에 지원하게 되었습니다."
							  },
							  {
							    "question": "2. 협업을 진행하며 겪었던 어려움과 이를 극복한 경험을 구체적으로 작성해 주세요",
							    "answer": "팀 프로젝트에서 의견 충돌이 있었지만, 서로의 의견을 듣고 타협점을 찾아 해결했습니다."
							  },
							  {
							    "question": "3. 평소 개발을 공부하며 만들어 보고 싶었던 웹사이트는 어떻게 되나요?",
							    "answer": "사용자들이 쉽게 정보를 공유할 수 있는 커뮤니티 사이트를 만들어보고 싶습니다."
							  }
							]
							"""
					)
				}
			)
		)
		@Valid @RequestBody @Size(max = 20, message = "질문은 최대 20개까지 가능합니다") List<@Valid ApplicationQuestionDto> questions) {
		
		// 입력 검증
		if (questions == null || questions.isEmpty()) {
			return ApiRes.failure(HttpStatus.BAD_REQUEST, "질문 목록이 비어있습니다.", ErrorCode.INVALID_ARGUMENT);
		}
		
		// 개별 질문/답변 길이 검증
		for (ApplicationQuestionDto question : questions) {
			if (question.getQuestion() == null || question.getQuestion().trim().isEmpty()) {
				return ApiRes.failure(HttpStatus.BAD_REQUEST, "질문이 비어있습니다.", ErrorCode.INVALID_ARGUMENT);
			}
			if (question.getAnswer() == null || question.getAnswer().trim().isEmpty()) {
				return ApiRes.failure(HttpStatus.BAD_REQUEST, "답변이 비어있습니다.", ErrorCode.INVALID_ARGUMENT);
			}
			if (question.getQuestion().length() > 500) {
				return ApiRes.failure(HttpStatus.BAD_REQUEST, "질문이 너무 깁니다 (최대 500자).", ErrorCode.INVALID_ARGUMENT);
			}
			if (question.getAnswer().length() > 5000) {
				return ApiRes.failure(HttpStatus.BAD_REQUEST, "답변이 너무 깁니다 (최대 5000자).", ErrorCode.INVALID_ARGUMENT);
			}
		}
		
		// 비동기 처리로 성능 개선
		try {
			ApplicationSummaryDto summary = applicationProcessingService
				.processApplicationAsync(questions)
				.get(45, TimeUnit.SECONDS); // 45초 타임아웃
			return ApiRes.success(summary, "동적 지원서 요약이 성공적으로 생성되었습니다.");
		} catch (java.util.concurrent.TimeoutException e) {
			log.error("AI processing timeout occurred");
			return ApiRes.failure(HttpStatus.REQUEST_TIMEOUT, 
				"AI 분석 시간이 초과되었습니다. 잠시 후 다시 시도해주세요.", 
				ErrorCode.INVALID_ARGUMENT);
		} catch (Exception e) {
			log.error("AI processing failed", e);
			return ApiRes.failure(HttpStatus.INTERNAL_SERVER_ERROR, 
				"AI 분석 중 오류가 발생했습니다.", 
				ErrorCode.INVALID_ARGUMENT);
		}
	}
	
	@Operation(
		summary = "배치 지원서 분석 (비동기)",
		description = "다중 지원서를 병렬로 처리하여 성능을 향상시킵니다. 최대 10개까지 동시 처리 가능합니다."
	)
	@PostMapping("/batch-analyze")
	@RequireAdmin
	public ApiRes<List<ApplicationSummaryDto>> analyzeBatchApplications(
		@io.swagger.v3.oas.annotations.parameters.RequestBody(
			description = "지원서 목록 (최대 10개)",
			required = true
		)
		@Valid @RequestBody @Size(max = 10, message = "배치는 최대 10개까지 가능합니다") 
		List<@Valid @Size(max = 20, message = "질문은 최대 20개까지 가능합니다") 
			List<@Valid ApplicationQuestionDto>> applicationsList) {
		
		if (applicationsList == null || applicationsList.isEmpty()) {
			return ApiRes.failure(HttpStatus.BAD_REQUEST, "지원서 목록이 비어있습니다.", ErrorCode.INVALID_ARGUMENT);
		}
		
		try {
			List<ApplicationSummaryDto> summaries = applicationProcessingService
				.processBatchApplicationsAsync(applicationsList)
				.get(120, TimeUnit.SECONDS); // 2분 타임아웃 (배치는 더 여유있게)
			
			return ApiRes.success(summaries, String.format("%d개 지원서 배치 분석이 완료되었습니다.", summaries.size()));
			
		} catch (java.util.concurrent.TimeoutException e) {
			log.error("Batch AI processing timeout occurred for {} applications", applicationsList.size());
			return ApiRes.failure(HttpStatus.REQUEST_TIMEOUT, 
				"배치 AI 분석 시간이 초과되었습니다. 개수를 줄이거나 잠시 후 다시 시도해주세요.", 
				ErrorCode.INVALID_ARGUMENT);
		} catch (Exception e) {
			log.error("Batch AI processing failed for {} applications", applicationsList.size(), e);
			return ApiRes.failure(HttpStatus.INTERNAL_SERVER_ERROR, 
				"배치 AI 분석 중 오류가 발생했습니다.", 
				ErrorCode.INVALID_ARGUMENT);
		}
	}
}