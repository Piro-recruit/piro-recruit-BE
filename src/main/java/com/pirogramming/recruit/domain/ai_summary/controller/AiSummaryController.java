package com.pirogramming.recruit.domain.ai_summary.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pirogramming.recruit.domain.ai_summary.dto.ApplicationQuestionDto;
import com.pirogramming.recruit.domain.ai_summary.dto.ApplicationSummaryDto;
import com.pirogramming.recruit.domain.ai_summary.service.ApplicationProcessingService;
import com.pirogramming.recruit.global.exception.ApiRes;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "AI 지원서 요약", description = "대학생 IT 개발 동아리 지원서 AI 자동 분석 및 평가 API")
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
	public ApiRes<ApplicationSummaryDto> analyzeDynamicApplication(
		@io.swagger.v3.oas.annotations.parameters.RequestBody(
			description = "지원서 문항과 답변 목록",
			required = true,
			content = @Content(
				mediaType = "application/json",
				schema = @Schema(implementation = ApplicationQuestionDto.class),
				examples = {
					@ExampleObject(
						name = "컴공과 전공자 지원서",
						value = """
							[
							  {
							    "question": "학과 및 학년",
							    "answer": "컴퓨터공학과 2학년"
							  },
							  {
							    "question": "자기소개서",
							    "answer": "안녕하세요. 웹 개발에 관심이 많은 컴공과 2학년 학생입니다. 혼자 공부하다가 팀원들과 함께 프로젝트를 진행해보고 싶어 지원하게 되었습니다."
							  },
							  {
							    "question": "프로그래밍 경험",
							    "answer": "C언어, Java 수업을 들었고, 개인적으로 Python과 JavaScript를 공부했습니다. 간단한 웹사이트를 만들어본 경험이 있습니다."
							  },
							  {
							    "question": "팀 프로젝트 경험",
							    "answer": "학교 과제로 4명이서 도서관 관리 시스템을 만들었습니다. 제가 데이터베이스 설계와 백엔드를 담당했습니다."
							  },
							  {
							    "question": "지원동기",
							    "answer": "혼자 공부하는 것보다 팀원들과 함께 더 큰 프로젝트를 만들어보고 싶습니다. 서로 가르쳐주고 배우면서 성장하고 싶어요."
							  }
							]
							"""
					),
					@ExampleObject(
						name = "비전공자 지원서",
						value = """
							[
							  {
							    "question": "학과 및 학년",
							    "answer": "경영학과 3학년"
							  },
							  {
							    "question": "자기소개서",
							    "answer": "경영학과지만 IT 분야에 관심이 많아 독학으로 프로그래밍을 공부하고 있습니다. 열정과 끈기로 부족한 부분을 채워나가겠습니다."
							  },
							  {
							    "question": "프로그래밍 학습 경험",
							    "answer": "온라인 강의로 HTML, CSS, JavaScript를 6개월간 학습했습니다. 개인 포트폴리오 웹사이트를 만들고 GitHub에 올렸습니다."
							  },
							  {
							    "question": "동아리 활동 경험",
							    "answer": "창업동아리에서 기획팀으로 활동하며 팀원들과 협업하는 방법을 배웠습니다. 의견 충돌이 있을 때 중재 역할을 맡기도 했습니다."
							  },
							  {
							    "question": "지원동기",
							    "answer": "비전공자이지만 IT에 대한 열정이 있습니다. 기술적으로 배우고 싶고, 다양한 배경의 사람들과 협업하며 성장하고 싶습니다."
							  }
							]
							"""
					)
				}
			)
		)
		@RequestBody List<ApplicationQuestionDto> questions) {
		ApplicationSummaryDto summary = applicationProcessingService.processApplication(questions);
		return ApiRes.success(summary, "동적 지원서 요약이 성공적으로 생성되었습니다.");
	}
}