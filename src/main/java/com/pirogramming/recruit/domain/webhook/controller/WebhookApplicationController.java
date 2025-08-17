package com.pirogramming.recruit.domain.webhook.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.pirogramming.recruit.domain.webhook.dto.WebhookApplicationRequest;
import com.pirogramming.recruit.domain.webhook.dto.WebhookApplicationResponse;
import com.pirogramming.recruit.domain.webhook.entity.WebhookApplication;
import com.pirogramming.recruit.domain.webhook.service.WebhookApplicationService;
import com.pirogramming.recruit.global.exception.ApiRes;
import com.pirogramming.recruit.global.exception.code.ErrorCode;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.pirogramming.recruit.global.security.RequireAdmin;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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

        log.info("웹훅 지원서 수신 - 폼ID: {}, 이메일: {}, 이름: {}",
                request.getFormId(), request.getApplicantEmail(), request.getApplicantName());

        WebhookApplicationResponse response = webhookApplicationService.processWebhookApplication(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiRes.success(response, "지원서가 성공적으로 저장되었습니다."));
    }

    // 전체 지원서 목록 조회
    @GetMapping
    @RequireAdmin
    @Operation(summary = "전체 지원서 조회", description = "저장된 모든 지원서를 최신순으로 조회합니다.")
    public ResponseEntity<ApiRes<List<WebhookApplicationResponse>>> getAllApplications() {

        List<WebhookApplicationResponse> applications = webhookApplicationService.getAllApplications();

        return ResponseEntity.ok(
                ApiRes.success(applications, applications.size() + "개의 지원서를 조회했습니다.")
        );
    }

    // 구글 폼별 지원서 목록 조회 (구글 폼 ID)
    @GetMapping("/google-form/{googleFormId}")
    @RequireAdmin
    @Operation(summary = "구글 폼별 지원서 조회", description = "특정 구글 폼의 모든 지원서를 조회합니다.")
    public ResponseEntity<ApiRes<List<WebhookApplicationResponse>>> getApplicationsByGoogleForm(
            @Parameter(description = "구글 폼 ID") @PathVariable Long googleFormId) {

        List<WebhookApplicationResponse> applications = webhookApplicationService.getApplicationsByGoogleForm(googleFormId);

        return ResponseEntity.ok(
                ApiRes.success(applications, "구글 폼 " + googleFormId + "의 지원서 " + applications.size() + "개를 조회했습니다.")
        );
    }

    // 폼 ID별 지원서 목록 조회
    @GetMapping("/form-id/{formId}")
    @RequireAdmin
    @Operation(summary = "폼 ID별 지원서 조회", description = "특정 폼 ID의 모든 지원서를 조회합니다.")
    public ResponseEntity<ApiRes<List<WebhookApplicationResponse>>> getApplicationsByFormId(
        @Parameter(description = "구글 폼 식별자") @PathVariable String formId) {

        List<WebhookApplicationResponse> applications = webhookApplicationService.getApplicationsByFormId(formId);

        return ResponseEntity.ok(
                ApiRes.success(applications, "폼 " + formId + "의 지원서 " + applications.size() + "개를 조회했습니다.")
        );
    }

    // 특정 지원서 조회 (ID 기준)
    @GetMapping("/id/{id}")
    @RequireAdmin
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
    @RequireAdmin
    @Operation(summary = "이메일로 지원서 조회", description = "이메일을 기준으로 지원서를 조회합니다.")
    public ResponseEntity<ApiRes<WebhookApplicationResponse>> getApplicationByEmail(
            @Parameter(description = "지원자 이메일") @RequestParam String email) {

        return webhookApplicationService.getApplicationByEmail(email)
                .map(application -> ResponseEntity.ok(ApiRes.success(application)))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiRes.failure(HttpStatus.NOT_FOUND, ErrorCode.WEBHOOK_APPLICATION_NOT_FOUND)));
    }

    // 구글 폼별 + 이메일로 지원서 조회
    @GetMapping("/google-form/{googleFormId}/by-email")
    @RequireAdmin
    @Operation(summary = "구글 폼별 이메일로 지원서 조회", description = "특정 구글 폼에서 이메일을 기준으로 지원서를 조회합니다.")
    public ResponseEntity<ApiRes<WebhookApplicationResponse>> getApplicationByGoogleFormAndEmail(
            @Parameter(description = "구글 폼 ID") @PathVariable Long googleFormId,
            @Parameter(description = "지원자 이메일") @RequestParam String email) {

        return webhookApplicationService.getApplicationByGoogleFormAndEmail(googleFormId, email)
                .map(application -> ResponseEntity.ok(ApiRes.success(application)))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiRes.failure(HttpStatus.NOT_FOUND, ErrorCode.WEBHOOK_APPLICATION_NOT_FOUND)));
    }

    // 폼 ID별 + 이메일로 지원서 조회
    @GetMapping("/form-id/{formId}/by-email")
    @RequireAdmin
    @Operation(summary = "폼 ID별 이메일로 지원서 조회", description = "특정 폼 ID에서 이메일을 기준으로 지원서를 조회합니다.")
    public ResponseEntity<ApiRes<WebhookApplicationResponse>> getApplicationByFormIdAndEmail(
            @Parameter(description = "구글 폼 ID") @PathVariable String formId,
            @Parameter(description = "지원자 이메일") @RequestParam String email) {

        return webhookApplicationService.getApplicationByFormIdAndEmail(formId, email)
                .map(application -> ResponseEntity.ok(ApiRes.success(application)))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiRes.failure(HttpStatus.NOT_FOUND, ErrorCode.WEBHOOK_APPLICATION_NOT_FOUND)));
    }

    // 처리 상태별 지원서 조회
    @GetMapping("/by-status")
    @RequireAdmin
    @Operation(summary = "상태별 지원서 조회", description = "처리 상태를 기준으로 지원서를 조회합니다.")
    public ResponseEntity<ApiRes<List<WebhookApplicationResponse>>> getApplicationsByStatus(
            @Parameter(description = "처리 상태 (PENDING, COMPLETED, FAILED)")
            @RequestParam WebhookApplication.ProcessingStatus status) {

        List<WebhookApplicationResponse> applications = webhookApplicationService.getApplicationsByStatus(status);

        return ResponseEntity.ok(
                ApiRes.success(applications, status + " 상태의 지원서 " + applications.size() + "개를 조회했습니다.")
        );
    }

    // 구글 폼별 + 상태별 지원서 조회
    @GetMapping("/google-form/{googleFormId}/by-status")
    @RequireAdmin
    @Operation(summary = "구글 폼별 상태별 지원서 조회", description = "특정 구글 폼에서 처리 상태를 기준으로 지원서를 조회합니다.")
    public ResponseEntity<ApiRes<List<WebhookApplicationResponse>>> getApplicationsByGoogleFormAndStatus(
            @Parameter(description = "구글 폼 ID") @PathVariable Long googleFormId,
            @Parameter(description = "처리 상태 (PENDING, COMPLETED, FAILED)")
            @RequestParam WebhookApplication.ProcessingStatus status) {

        List<WebhookApplicationResponse> applications =
                webhookApplicationService.getApplicationsByGoogleFormAndStatus(googleFormId, status);

        return ResponseEntity.ok(
                ApiRes.success(applications,
                        "구글 폼 " + googleFormId + "의 " + status + " 상태 지원서 " + applications.size() + "개를 조회했습니다.")
        );
    }

    // 지원서 제출 여부 확인
    @GetMapping("/check")
    @RequireAdmin
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

    // 구글 폼별 지원서 제출 여부 확인
    @GetMapping("/google-form/{googleFormId}/check")
    @RequireAdmin
    @Operation(summary = "구글 폼별 지원서 제출 여부 확인", description = "특정 구글 폼에서 이메일을 기준으로 지원서 제출 여부를 확인합니다.")
    public ResponseEntity<ApiRes<Map<String, Object>>> checkApplicationStatusForGoogleForm(
            @Parameter(description = "구글 폼 ID") @PathVariable Long googleFormId,
            @Parameter(description = "확인할 이메일") @RequestParam String email) {

        boolean isSubmitted = webhookApplicationService.isApplicationSubmittedForGoogleForm(googleFormId, email);

        Map<String, Object> result = Map.of(
                "googleFormId", googleFormId,
                "email", email,
                "submitted", isSubmitted,
                "status", isSubmitted ? "submitted" : "not_submitted"
        );

        return ResponseEntity.ok(ApiRes.success(result));
    }

    // 폼 ID별 지원서 제출 여부 확인
    @GetMapping("/form-id/{formId}/check")
    @RequireAdmin
    @Operation(summary = "폼 ID별 지원서 제출 여부 확인", description = "특정 폼 ID에서 이메일을 기준으로 지원서 제출 여부를 확인합니다.")
    public ResponseEntity<ApiRes<Map<String, Object>>> checkApplicationStatusForFormId(
            @Parameter(description = "구글 폼 ID") @PathVariable String formId,
            @Parameter(description = "확인할 이메일") @RequestParam String email) {

        boolean isSubmitted = webhookApplicationService.isApplicationSubmittedForFormId(formId, email);

        Map<String, Object> result = Map.of(
                "formId", formId,
                "email", email,
                "submitted", isSubmitted,
                "status", isSubmitted ? "submitted" : "not_submitted"
        );

        return ResponseEntity.ok(ApiRes.success(result));
    }

    // 대기 중인 지원서 개수 조회
    @GetMapping("/pending-count")
    @RequireAdmin
    @Operation(summary = "대기 중인 지원서 개수", description = "처리 대기 중인 지원서의 개수를 조회합니다.")
    public ResponseEntity<ApiRes<Map<String, Object>>> getPendingApplicationCount() {

        long pendingCount = webhookApplicationService.getPendingApplicationCount();

        Map<String, Object> result = Map.of(
                "pendingCount", pendingCount,
                "message", "처리 대기 중인 지원서 " + pendingCount + "개"
        );

        return ResponseEntity.ok(ApiRes.success(result));
    }

    // 구글 폼별 지원서 개수 조회
    @GetMapping("/google-form/{googleFormId}/count")
    @RequireAdmin
    @Operation(summary = "구글 폼별 지원서 개수", description = "특정 구글 폼의 총 지원서 개수를 조회합니다.")
    public ResponseEntity<ApiRes<Map<String, Object>>> getApplicationCountByGoogleForm(
            @Parameter(description = "구글 폼 ID") @PathVariable Long googleFormId) {

        long applicationCount = webhookApplicationService.getApplicationCountByGoogleForm(googleFormId);

        Map<String, Object> result = Map.of(
                "googleFormId", googleFormId,
                "applicationCount", applicationCount,
                "message", "구글 폼 " + googleFormId + "의 지원서 " + applicationCount + "개"
        );

        return ResponseEntity.ok(ApiRes.success(result));
    }

    // 폼 ID별 지원서 개수 조회
    @GetMapping("/form-id/{formId}/count")
    @RequireAdmin
    @Operation(summary = "폼 ID별 지원서 개수", description = "특정 폼 ID의 총 지원서 개수를 조회합니다.")
    public ResponseEntity<ApiRes<Map<String, Object>>> getApplicationCountByFormId(
            @Parameter(description = "구글 폼 ID") @PathVariable String formId) {

        long applicationCount = webhookApplicationService.getApplicationCountByFormId(formId);

        Map<String, Object> result = Map.of(
                "formId", formId,
                "applicationCount", applicationCount,
                "message", "폼 " + formId + "의 지원서 " + applicationCount + "개"
        );

        return ResponseEntity.ok(ApiRes.success(result));
    }

    // 상태별 통계 조회
    @GetMapping("/statistics")
    @RequireAdmin
    @Operation(summary = "상태별 통계 조회", description = "전체 지원서의 상태별 통계를 조회합니다.")
    public ResponseEntity<ApiRes<Map<WebhookApplication.ProcessingStatus, Long>>> getStatusStatistics() {

        Map<WebhookApplication.ProcessingStatus, Long> statistics = webhookApplicationService.getStatusStatistics();

        return ResponseEntity.ok(ApiRes.success(statistics, "상태별 통계를 조회했습니다."));
    }

    // 구글 폼별 상태별 통계 조회
    @GetMapping("/google-form/{googleFormId}/statistics")
    @RequireAdmin
    @Operation(summary = "구글 폼별 상태별 통계 조회", description = "특정 구글 폼의 상태별 통계를 조회합니다.")
    public ResponseEntity<ApiRes<Map<WebhookApplication.ProcessingStatus, Long>>> getStatusStatisticsByGoogleForm(
            @Parameter(description = "구글 폼 ID") @PathVariable Long googleFormId) {

        Map<WebhookApplication.ProcessingStatus, Long> statistics =
                webhookApplicationService.getStatusStatisticsByGoogleForm(googleFormId);

        return ResponseEntity.ok(ApiRes.success(statistics, "구글 폼 " + googleFormId + "의 상태별 통계를 조회했습니다."));
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