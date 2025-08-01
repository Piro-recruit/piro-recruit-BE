package com.pirogramming.recruit.domain.webhook.controller;

import com.pirogramming.recruit.domain.webhook.dto.WebhookApplicationRequest;
import com.pirogramming.recruit.domain.webhook.dto.WebhookApplicationResponse;
import com.pirogramming.recruit.domain.webhook.entity.WebhookApplication;
import com.pirogramming.recruit.domain.webhook.service.WebhookApplicationService;
import com.pirogramming.recruit.global.exception.ApiRes;
import com.pirogramming.recruit.global.exception.code.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/webhook/applications")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Webhook Application", description = "구글 폼 웹훅 지원서 관리 API")
public class WebhookApplicationController {

    private final WebhookApplicationService webhookApplicationService;

    // 구글 폼 웹훅으로부터 지원서 데이터 수신 및 저장
    @PostMapping("/receive")
    @Operation(summary = "웹훅 지원서 수신", description = "구글 폼에서 전송된 지원서 데이터를 받아 저장합니다.")
    public ResponseEntity<ApiRes<WebhookApplicationResponse>> receiveWebhookApplication(
            @Valid @RequestBody WebhookApplicationRequest request) {

        log.info("웹훅 지원서 수신 - 리크루팅ID: {}, 이메일: {}, 이름: {}",
                request.getRecruitmentId(), request.getApplicantEmail(), request.getApplicantName());

        WebhookApplicationResponse response = webhookApplicationService.processWebhookApplication(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiRes.success(response, "지원서가 성공적으로 저장되었습니다."));
    }

    // 전체 지원서 목록 조회
    @GetMapping
    @Operation(summary = "전체 지원서 조회", description = "저장된 모든 지원서를 최신순으로 조회합니다.")
    public ResponseEntity<ApiRes<List<WebhookApplicationResponse>>> getAllApplications() {

        List<WebhookApplicationResponse> applications = webhookApplicationService.getAllApplications();

        return ResponseEntity.ok(
                ApiRes.success(applications, applications.size() + "개의 지원서를 조회했습니다.")
        );
    }

    // 리크루팅별 지원서 목록 조회
    @GetMapping("/recruitment/{recruitmentId}")
    @Operation(summary = "리크루팅별 지원서 조회", description = "특정 리크루팅의 모든 지원서를 조회합니다.")
    public ResponseEntity<ApiRes<List<WebhookApplicationResponse>>> getApplicationsByRecruitment(
            @Parameter(description = "리크루팅 ID") @PathVariable Long recruitmentId) {

        List<WebhookApplicationResponse> applications = webhookApplicationService.getApplicationsByRecruitment(recruitmentId);

        return ResponseEntity.ok(
                ApiRes.success(applications, "리크루팅 " + recruitmentId + "의 지원서 " + applications.size() + "개를 조회했습니다.")
        );
    }

    // 특정 지원서 조회 (ID 기준)
    @GetMapping("/{id}")
    @Operation(summary = "특정 지원서 조회", description = "ID를 기준으로 특정 지원서를 조회합니다.")
    public ResponseEntity<ApiRes<WebhookApplicationResponse>> getApplicationById(
            @Parameter(description = "지원서 ID") @PathVariable Long id) {

        return webhookApplicationService.getApplicationById(id)
                .map(application -> ResponseEntity.ok(ApiRes.success(application)))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiRes.failure(HttpStatus.NOT_FOUND, ErrorCode.WEBHOOK_APPLICATION_NOT_FOUND)));
    }

    // 이메일로 지원서 조회
    @GetMapping("/by-email")
    @Operation(summary = "이메일로 지원서 조회", description = "이메일을 기준으로 지원서를 조회합니다.")
    public ResponseEntity<ApiRes<WebhookApplicationResponse>> getApplicationByEmail(
            @Parameter(description = "지원자 이메일") @RequestParam String email) {

        return webhookApplicationService.getApplicationByEmail(email)
                .map(application -> ResponseEntity.ok(ApiRes.success(application)))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiRes.failure(HttpStatus.NOT_FOUND, ErrorCode.WEBHOOK_APPLICATION_NOT_FOUND)));
    }

    // 리크루팅별 + 이메일로 지원서 조회
    @GetMapping("/recruitment/{recruitmentId}/by-email")
    @Operation(summary = "리크루팅별 이메일로 지원서 조회", description = "특정 리크루팅에서 이메일을 기준으로 지원서를 조회합니다.")
    public ResponseEntity<ApiRes<WebhookApplicationResponse>> getApplicationByRecruitmentAndEmail(
            @Parameter(description = "리크루팅 ID") @PathVariable Long recruitmentId,
            @Parameter(description = "지원자 이메일") @RequestParam String email) {

        return webhookApplicationService.getApplicationByRecruitmentAndEmail(recruitmentId, email)
                .map(application -> ResponseEntity.ok(ApiRes.success(application)))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiRes.failure(HttpStatus.NOT_FOUND, ErrorCode.WEBHOOK_APPLICATION_NOT_FOUND)));
    }

    // 처리 상태별 지원서 조회
    @GetMapping("/by-status")
    @Operation(summary = "상태별 지원서 조회", description = "처리 상태를 기준으로 지원서를 조회합니다.")
    public ResponseEntity<ApiRes<List<WebhookApplicationResponse>>> getApplicationsByStatus(
            @Parameter(description = "처리 상태 (PENDING, COMPLETED, FAILED)")
            @RequestParam WebhookApplication.ProcessingStatus status) {

        List<WebhookApplicationResponse> applications = webhookApplicationService.getApplicationsByStatus(status);

        return ResponseEntity.ok(
                ApiRes.success(applications, status + " 상태의 지원서 " + applications.size() + "개를 조회했습니다.")
        );
    }

    // 리크루팅별 + 상태별 지원서 조회
    @GetMapping("/recruitment/{recruitmentId}/by-status")
    @Operation(summary = "리크루팅별 상태별 지원서 조회", description = "특정 리크루팅에서 처리 상태를 기준으로 지원서를 조회합니다.")
    public ResponseEntity<ApiRes<List<WebhookApplicationResponse>>> getApplicationsByRecruitmentAndStatus(
            @Parameter(description = "리크루팅 ID") @PathVariable Long recruitmentId,
            @Parameter(description = "처리 상태 (PENDING, COMPLETED, FAILED)")
            @RequestParam WebhookApplication.ProcessingStatus status) {

        List<WebhookApplicationResponse> applications =
                webhookApplicationService.getApplicationsByRecruitmentAndStatus(recruitmentId, status);

        return ResponseEntity.ok(
                ApiRes.success(applications,
                        "리크루팅 " + recruitmentId + "의 " + status + " 상태 지원서 " + applications.size() + "개를 조회했습니다.")
        );
    }

    // 지원서 제출 여부 확인
    @GetMapping("/check")
    @Operation(summary = "지원서 제출 여부 확인", description = "이메일을 기준으로 지원서 제출 여부를 확인합니다.")
    public ResponseEntity<ApiRes<Map<String, Object>>> checkApplicationStatus(
            @Parameter(description = "확인할 이메일") @RequestParam String email) {

        boolean isSubmitted = webhookApplicationService.isApplicationSubmitted(email);

        Map<String, Object> result = Map.of(
                "email", email,
                "submitted", isSubmitted,
                "status", isSubmitted ? "submitted" : "not_submitted"
        );

        return ResponseEntity.ok(ApiRes.success(result));
    }

    // 리크루팅별 지원서 제출 여부 확인
    @GetMapping("/recruitment/{recruitmentId}/check")
    @Operation(summary = "리크루팅별 지원서 제출 여부 확인", description = "특정 리크루팅에서 이메일을 기준으로 지원서 제출 여부를 확인합니다.")
    public ResponseEntity<ApiRes<Map<String, Object>>> checkApplicationStatusForRecruitment(
            @Parameter(description = "리크루팅 ID") @PathVariable Long recruitmentId,
            @Parameter(description = "확인할 이메일") @RequestParam String email) {

        boolean isSubmitted = webhookApplicationService.isApplicationSubmittedForRecruitment(recruitmentId, email);

        Map<String, Object> result = Map.of(
                "recruitmentId", recruitmentId,
                "email", email,
                "submitted", isSubmitted,
                "status", isSubmitted ? "submitted" : "not_submitted"
        );

        return ResponseEntity.ok(ApiRes.success(result));
    }

    // 대기 중인 지원서 개수 조회
    @GetMapping("/pending-count")
    @Operation(summary = "대기 중인 지원서 개수", description = "처리 대기 중인 지원서의 개수를 조회합니다.")
    public ResponseEntity<ApiRes<Map<String, Object>>> getPendingApplicationCount() {

        long pendingCount = webhookApplicationService.getPendingApplicationCount();

        Map<String, Object> result = Map.of(
                "pendingCount", pendingCount,
                "message", "처리 대기 중인 지원서 " + pendingCount + "개"
        );

        return ResponseEntity.ok(ApiRes.success(result));
    }

    // 리크루팅별 지원서 개수 조회
    @GetMapping("/recruitment/{recruitmentId}/count")
    @Operation(summary = "리크루팅별 지원서 개수", description = "특정 리크루팅의 총 지원서 개수를 조회합니다.")
    public ResponseEntity<ApiRes<Map<String, Object>>> getApplicationCountByRecruitment(
            @Parameter(description = "리크루팅 ID") @PathVariable Long recruitmentId) {

        long applicationCount = webhookApplicationService.getApplicationCountByRecruitment(recruitmentId);

        Map<String, Object> result = Map.of(
                "recruitmentId", recruitmentId,
                "applicationCount", applicationCount,
                "message", "리크루팅 " + recruitmentId + "의 지원서 " + applicationCount + "개"
        );

        return ResponseEntity.ok(ApiRes.success(result));
    }

    // 상태별 통계 조회
    @GetMapping("/statistics")
    @Operation(summary = "상태별 통계 조회", description = "전체 지원서의 상태별 통계를 조회합니다.")
    public ResponseEntity<ApiRes<Map<WebhookApplication.ProcessingStatus, Long>>> getStatusStatistics() {

        Map<WebhookApplication.ProcessingStatus, Long> statistics = webhookApplicationService.getStatusStatistics();

        return ResponseEntity.ok(ApiRes.success(statistics, "상태별 통계를 조회했습니다."));
    }

    // 리크루팅별 상태별 통계 조회
    @GetMapping("/recruitment/{recruitmentId}/statistics")
    @Operation(summary = "리크루팅별 상태별 통계 조회", description = "특정 리크루팅의 상태별 통계를 조회합니다.")
    public ResponseEntity<ApiRes<Map<WebhookApplication.ProcessingStatus, Long>>> getStatusStatisticsByRecruitment(
            @Parameter(description = "리크루팅 ID") @PathVariable Long recruitmentId) {

        Map<WebhookApplication.ProcessingStatus, Long> statistics =
                webhookApplicationService.getStatusStatisticsByRecruitment(recruitmentId);

        return ResponseEntity.ok(ApiRes.success(statistics, "리크루팅 " + recruitmentId + "의 상태별 통계를 조회했습니다."));
    }

    // 웹훅 연결 테스트용 엔드포인트
    @PostMapping("/test")
    @Operation(summary = "웹훅 연결 테스트", description = "Apps Script와의 연결을 테스트하기 위한 엔드포인트입니다.")
    public ResponseEntity<ApiRes<Map<String, Object>>> testWebhookConnection(
            @RequestBody(required = false) Map<String, Object> testData) {

        log.info("웹훅 연결 테스트 수신 - 데이터: {}", testData);

        Map<String, Object> response = Map.of(
                "status", "success",
                "message", "웹훅 연결이 정상적으로 작동합니다.",
                "timestamp", System.currentTimeMillis(),
                "receivedData", testData != null ? testData : Map.of()
        );

        return ResponseEntity.ok(ApiRes.success(response, "웹훅 테스트 성공"));
    }
}