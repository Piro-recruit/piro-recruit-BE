package com.pirogramming.recruit.domain.evaluation.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pirogramming.recruit.domain.admin.service.CustomUserDetails;
import com.pirogramming.recruit.domain.evaluation.dto.ApplicationEvaluationSummaryResponse;
import com.pirogramming.recruit.domain.evaluation.dto.EvaluationRequest;
import com.pirogramming.recruit.domain.evaluation.dto.EvaluationResponse;
import com.pirogramming.recruit.domain.evaluation.dto.EvaluationUpdateRequest;
import com.pirogramming.recruit.domain.evaluation.service.EvaluationService;
import com.pirogramming.recruit.global.exception.ApiRes;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/evaluations")
@RequiredArgsConstructor
@Tag(name = "Evaluation", description = "지원서 평가 관리 API")
public class EvaluationController {

    private final EvaluationService evaluationService;

    @PostMapping
    @Operation(
        summary = "평가 생성", 
        description = """
            지원서에 대한 새로운 평가를 생성합니다.
            
            **제약사항:**
            - 한 평가자는 동일한 지원서에 대해 하나의 평가만 작성 가능
            - 점수는 0점 이상 100점 이하만 허용
            - 평가 생성 시 해당 지원서의 평균 점수가 자동으로 업데이트됩니다
            
            **권한:** 인증된 Admin 사용자만 접근 가능
            """
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "평가 생성 성공",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiRes.class),
                examples = @ExampleObject(
                    name = "성공 응답",
                    value = """
                    {
                      "success": true,
                      "data": {
                        "id": 1,
                        "applicationId": 1,
                        "applicantName": "홍길동",
                        "evaluatorId": 1,
                        "evaluatorName": "평가자1",
                        "score": 85,
                        "comment": "전반적으로 우수한 지원서입니다.",
                        "createdAt": "2024-01-01T10:00:00",
                        "updatedAt": "2024-01-01T10:00:00"
                      },
                      "message": "'홍길동' 지원자에 대한 평가가 성공적으로 등록되었습니다. (점수: 85점)",
                      "status": 200,
                      "code": 0
                    }
                    """
                )
            )
        ),
        @ApiResponse(
            responseCode = "400", 
            description = "잘못된 요청 (유효성 검증 실패)",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "점수 범위 오류",
                    value = """
                    {
                      "success": false,
                      "data": null,
                      "message": "score: 점수는 100점 이하여야 합니다. 현재 입력된 값: 150",
                      "status": 400,
                      "code": 1001
                    }
                    """
                )
            )
        ),
        @ApiResponse(
            responseCode = "404", 
            description = "지원서를 찾을 수 없음",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "지원서 없음",
                    value = """
                    {
                      "success": false,
                      "data": null,
                      "message": "지원서 ID 999를 찾을 수 없습니다.",
                      "status": 404,
                      "code": 4005
                    }
                    """
                )
            )
        ),
        @ApiResponse(
            responseCode = "409", 
            description = "중복 평가 시도",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "중복 평가",
                    value = """
                    {
                      "success": false,
                      "data": null,
                      "message": "평가자 '김철수'는 이미 지원서 ID 1에 대한 평가를 등록하셨습니다.",
                      "status": 409,
                      "code": 4002
                    }
                    """
                )
            )
        )
    })
    public ResponseEntity<ApiRes<EvaluationResponse>> createEvaluation(
            @Parameter(description = "평가 생성 요청 데이터", required = true)
            @Valid @RequestBody EvaluationRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        EvaluationResponse response = evaluationService.createEvaluation(request, userDetails.getId());
        return ResponseEntity.ok(ApiRes.success(response, 
            String.format("'%s' 지원자에 대한 평가가 성공적으로 등록되었습니다. (점수: %d점)", 
                response.getApplicantName(), response.getScore())));
    }

    @PutMapping("/{evaluationId}")
    @Operation(
        summary = "평가 수정", 
        description = """
            기존 평가의 점수와 코멘트를 수정합니다.
            
            **제약사항:**
            - 본인이 작성한 평가만 수정 가능
            - 점수는 0점 이상 100점 이하만 허용
            - 평가 수정 시 해당 지원서의 평균 점수가 자동으로 재계산됩니다
            
            **권한:** 해당 평가를 작성한 Admin 사용자만 접근 가능
            """
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "평가 수정 성공",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "수정 성공",
                    value = """
                    {
                      "success": true,
                      "data": {
                        "id": 1,
                        "applicationId": 1,
                        "applicantName": "홍길동",
                        "evaluatorId": 1,
                        "evaluatorName": "평가자1",
                        "score": 90,
                        "comment": "재검토 결과 더 높은 점수를 주고 싶습니다.",
                        "createdAt": "2024-01-01T10:00:00",
                        "updatedAt": "2024-01-01T10:30:00"
                      },
                      "message": "'홍길동' 지원자에 대한 평가가 성공적으로 수정되었습니다. (변경된 점수: 90점)",
                      "status": 200,
                      "code": 0
                    }
                    """
                )
            )
        ),
        @ApiResponse(
            responseCode = "403", 
            description = "수정 권한 없음",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "권한 없음",
                    value = """
                    {
                      "success": false,
                      "data": null,
                      "message": "평가자 '이영희'는 평가 ID 5에 대한 권한이 없습니다. 본인이 작성한 평가만 수정/삭제할 수 있습니다.",
                      "status": 403,
                      "code": 4003
                    }
                    """
                )
            )
        ),
        @ApiResponse(
            responseCode = "404", 
            description = "평가를 찾을 수 없음",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "평가 없음",
                    value = """
                    {
                      "success": false,
                      "data": null,
                      "message": "평가 ID 999를 찾을 수 없습니다.",
                      "status": 404,
                      "code": 4001
                    }
                    """
                )
            )
        )
    })
    public ResponseEntity<ApiRes<EvaluationResponse>> updateEvaluation(
            @Parameter(description = "수정할 평가의 ID", example = "1", required = true)
            @PathVariable Long evaluationId,
            @Parameter(description = "평가 수정 요청 데이터", required = true)
            @Valid @RequestBody EvaluationUpdateRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        EvaluationResponse response = evaluationService.updateEvaluation(evaluationId, request, userDetails.getId());
        return ResponseEntity.ok(ApiRes.success(response, 
            String.format("'%s' 지원자에 대한 평가가 성공적으로 수정되었습니다. (변경된 점수: %d점)", 
                response.getApplicantName(), response.getScore())));
    }

    @DeleteMapping("/{evaluationId}")
    @Operation(
        summary = "평가 삭제", 
        description = """
            기존 평가를 삭제합니다.
            
            **제약사항:**
            - 본인이 작성한 평가만 삭제 가능
            - 평가 삭제 시 해당 지원서의 평균 점수가 자동으로 재계산됩니다
            
            **권한:** 해당 평가를 작성한 Admin 사용자만 접근 가능
            """
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "평가 삭제 성공"),
        @ApiResponse(responseCode = "403", description = "삭제 권한 없음"),
        @ApiResponse(responseCode = "404", description = "평가를 찾을 수 없음")
    })
    public ResponseEntity<ApiRes<Void>> deleteEvaluation(
            @Parameter(description = "삭제할 평가의 ID", example = "1", required = true)
            @PathVariable Long evaluationId,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        evaluationService.deleteEvaluation(evaluationId, userDetails.getId());
        return ResponseEntity.ok(ApiRes.success(null, 
            String.format("평가 ID %d가 성공적으로 삭제되었습니다.", evaluationId)));
    }

    @GetMapping("/{evaluationId}")
    @Operation(summary = "평가 조회", description = "특정 평가의 상세 정보를 조회합니다")
    public ResponseEntity<ApiRes<EvaluationResponse>> getEvaluation(@PathVariable Long evaluationId) {
        EvaluationResponse response = evaluationService.getEvaluation(evaluationId);
        return ResponseEntity.ok(ApiRes.success(response));
    }

    @GetMapping("/application/{applicationId}")
    @Operation(summary = "지원서별 평가 목록 조회", description = "특정 지원서에 대한 모든 평가를 조회합니다")
    public ResponseEntity<ApiRes<List<EvaluationResponse>>> getEvaluationsByApplication(
            @PathVariable Long applicationId) {
        
        List<EvaluationResponse> responses = evaluationService.getEvaluationsByApplication(applicationId);
        return ResponseEntity.ok(ApiRes.success(responses));
    }

    @GetMapping("/evaluator/my")
    @Operation(summary = "내 평가 목록 조회", description = "현재 로그인한 평가자의 모든 평가를 조회합니다")
    public ResponseEntity<ApiRes<List<EvaluationResponse>>> getMyEvaluations(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        List<EvaluationResponse> responses = evaluationService.getEvaluationsByEvaluator(userDetails.getId());
        return ResponseEntity.ok(ApiRes.success(responses));
    }

    @GetMapping("/application/{applicationId}/summary")
    @Operation(
        summary = "지원서 평가 요약", 
        description = """
            특정 지원서의 평가 요약 정보를 조회합니다.
            
            **포함 정보:**
            - 지원자 기본 정보 (ID, 이름)
            - 평균 점수 (모든 평가자의 점수 평균)
            - 총 평가 개수
            - 개별 평가 목록 (평가자, 점수, 코멘트 포함)
            
            **활용:** 지원자에 대한 전체적인 평가 현황 파악에 유용
            """
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "평가 요약 조회 성공",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "평가 요약",
                    value = """
                    {
                      "success": true,
                      "data": {
                        "applicationId": 1,
                        "applicantName": "홍길동",
                        "averageScore": 82.5,
                        "evaluationCount": 2,
                        "evaluations": [
                          {
                            "id": 1,
                            "applicationId": 1,
                            "applicantName": "홍길동",
                            "evaluatorId": 1,
                            "evaluatorName": "평가자1",
                            "score": 85,
                            "comment": "우수한 지원서입니다.",
                            "createdAt": "2024-01-01T10:00:00",
                            "updatedAt": "2024-01-01T10:00:00"
                          },
                          {
                            "id": 2,
                            "applicationId": 1,
                            "applicantName": "홍길동",
                            "evaluatorId": 2,
                            "evaluatorName": "평가자2",
                            "score": 80,
                            "comment": "괜찮은 수준입니다.",
                            "createdAt": "2024-01-01T11:00:00",
                            "updatedAt": "2024-01-01T11:00:00"
                          }
                        ]
                      },
                      "status": 200,
                      "code": 0
                    }
                    """
                )
            )
        ),
        @ApiResponse(responseCode = "404", description = "지원서를 찾을 수 없음")
    })
    public ResponseEntity<ApiRes<ApplicationEvaluationSummaryResponse>> getApplicationEvaluationSummary(
            @Parameter(description = "평가 요약을 조회할 지원서 ID", example = "1", required = true)
            @PathVariable Long applicationId) {
        
        ApplicationEvaluationSummaryResponse response = evaluationService.getApplicationEvaluationSummary(applicationId);
        return ResponseEntity.ok(ApiRes.success(response));
    }
}