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
    @Operation(summary = "평가 생성", description = "지원서에 대한 평가를 생성합니다")
    public ResponseEntity<ApiRes<EvaluationResponse>> createEvaluation(
            @Valid @RequestBody EvaluationRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        EvaluationResponse response = evaluationService.createEvaluation(request, userDetails.getId());
        return ResponseEntity.ok(ApiRes.success(response, "평가가 성공적으로 생성되었습니다"));
    }

    @PutMapping("/{evaluationId}")
    @Operation(summary = "평가 수정", description = "기존 평가를 수정합니다")
    public ResponseEntity<ApiRes<EvaluationResponse>> updateEvaluation(
            @PathVariable Long evaluationId,
            @Valid @RequestBody EvaluationUpdateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        EvaluationResponse response = evaluationService.updateEvaluation(evaluationId, request, userDetails.getId());
        return ResponseEntity.ok(ApiRes.success(response, "평가가 성공적으로 수정되었습니다"));
    }

    @DeleteMapping("/{evaluationId}")
    @Operation(summary = "평가 삭제", description = "기존 평가를 삭제합니다")
    public ResponseEntity<ApiRes<Void>> deleteEvaluation(
            @PathVariable Long evaluationId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        evaluationService.deleteEvaluation(evaluationId, userDetails.getId());
        return ResponseEntity.ok(ApiRes.success(null, "평가가 성공적으로 삭제되었습니다"));
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
    @Operation(summary = "지원서 평가 요약", description = "특정 지원서의 평가 요약 정보를 조회합니다 (평균 점수, 평가 개수, 평가 목록)")
    public ResponseEntity<ApiRes<ApplicationEvaluationSummaryResponse>> getApplicationEvaluationSummary(
            @PathVariable Long applicationId) {
        
        ApplicationEvaluationSummaryResponse response = evaluationService.getApplicationEvaluationSummary(applicationId);
        return ResponseEntity.ok(ApiRes.success(response));
    }
}