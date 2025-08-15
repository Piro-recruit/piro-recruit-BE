package com.pirogramming.recruit.domain.admin.controller;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pirogramming.recruit.domain.webhook.dto.WebhookApplicationResponse;
import com.pirogramming.recruit.domain.webhook.entity.WebhookApplication;
import com.pirogramming.recruit.domain.webhook.service.WebhookApplicationService;
import com.pirogramming.recruit.global.exception.ApiRes;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/admin/applications")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Application Management", description = "지원서 관리 API (합격 처리)")
@PreAuthorize("hasRole('ROOT') or hasRole('GENERAL')")
public class ApplicationManagementController {

    private final WebhookApplicationService webhookApplicationService;

    // 개별 합격 상태 변경
    @PutMapping("/{id}/pass-status")
    @Operation(summary = "합격 상태 변경", description = "지원자의 합격 상태를 변경합니다.")
    public ResponseEntity<ApiRes<WebhookApplicationResponse>> updatePassStatus(
            @Parameter(description = "지원서 ID") @PathVariable Long id,
            @Valid @RequestBody PassStatusUpdateRequest request) {

        log.info("합격 상태 변경 요청 - ID: {}, 상태: {}", id, request.getPassStatus());

        WebhookApplication application = webhookApplicationService.updatePassStatus(id, request.getPassStatus());
        WebhookApplicationResponse response = WebhookApplicationResponse.from(application);

        return ResponseEntity.ok(
                ApiRes.success(response, "합격 상태가 변경되었습니다.")
        );
    }

    // 일괄 합격 상태 변경
    @PutMapping("/all/pass-status")
    @Operation(summary = "일괄 합격 처리", description = "여러 지원자의 합격 상태를 일괄 변경합니다.")
    public ResponseEntity<ApiRes<List<WebhookApplicationResponse>>> updatePassStatusAll(
            @Valid @RequestBody AllPassStatusUpdateRequest request) {

        log.info("일괄 합격 상태 변경 요청 - 대상: {} 건, 상태: {}",
                request.getApplicationIds().size(), request.getPassStatus());

        List<WebhookApplication> applications = webhookApplicationService.updatePassStatusAll(
                request.getApplicationIds(), request.getPassStatus());

        List<WebhookApplicationResponse> responses = applications.stream()
                .map(WebhookApplicationResponse::from)
                .collect(Collectors.toList());

        return ResponseEntity.ok(
                ApiRes.success(responses,
                        String.format("%d명의 합격 상태가 변경되었습니다.", responses.size()))
        );
    }

    // 합격 상태별 지원서 조회
    @GetMapping("/pass-status/{status}")
    @Operation(summary = "합격 상태별 조회", description = "특정 합격 상태의 지원자들을 조회합니다.")
    public ResponseEntity<ApiRes<List<WebhookApplicationResponse>>> getApplicationsByPassStatus(
            @Parameter(description = "합격 상태") @PathVariable WebhookApplication.PassStatus status) {

        List<WebhookApplication> applications = webhookApplicationService.getApplicationsByPassStatus(status);
        List<WebhookApplicationResponse> responses = applications.stream()
                .map(WebhookApplicationResponse::from)
                .collect(Collectors.toList());

        return ResponseEntity.ok(
                ApiRes.success(responses,
                        String.format("%s 상태의 지원자 %d명을 조회했습니다.", status, responses.size()))
        );
    }

    // 합격 상태 통계 조회
    @GetMapping("/pass-status/statistics")
    @Operation(summary = "합격 상태 통계", description = "전체 지원자의 합격 상태 통계를 조회합니다.")
    public ResponseEntity<ApiRes<Map<WebhookApplication.PassStatus, Long>>> getPassStatusStatistics() {

        Map<WebhookApplication.PassStatus, Long> statistics = webhookApplicationService.getPassStatusStatistics();

        return ResponseEntity.ok(
                ApiRes.success(statistics, "합격 상태 통계를 조회했습니다.")
        );
    }

    // 구글 폼별 합격 상태 통계 조회
    @GetMapping("/google-form/{googleFormId}/pass-status/statistics")
    @Operation(summary = "구글 폼별 합격 상태 통계", description = "특정 구글 폼의 합격 상태 통계를 조회합니다.")
    public ResponseEntity<ApiRes<Map<WebhookApplication.PassStatus, Long>>> getPassStatusStatisticsByGoogleForm(
            @Parameter(description = "구글 폼 ID") @PathVariable Long googleFormId) {

        Map<WebhookApplication.PassStatus, Long> statistics =
                webhookApplicationService.getPassStatusStatisticsByGoogleForm(googleFormId);

        return ResponseEntity.ok(
                ApiRes.success(statistics,
                        String.format("구글 폼 %d의 합격 상태 통계를 조회했습니다.", googleFormId))
        );
    }

    // 구글 폼별 + 합격 상태별 지원서 조회
    @GetMapping("/google-form/{googleFormId}/pass-status/{status}")
    @Operation(summary = "구글 폼별 합격 상태별 조회", description = "특정 구글 폼의 특정 합격 상태 지원자들을 조회합니다.")
    public ResponseEntity<ApiRes<List<WebhookApplicationResponse>>> getApplicationsByGoogleFormAndPassStatus(
            @Parameter(description = "구글 폼 ID") @PathVariable Long googleFormId,
            @Parameter(description = "합격 상태") @PathVariable WebhookApplication.PassStatus status) {

        List<WebhookApplication> applications =
                webhookApplicationService.getApplicationsByGoogleFormAndPassStatus(googleFormId, status);

        List<WebhookApplicationResponse> responses = applications.stream()
                .map(WebhookApplicationResponse::from)
                .collect(Collectors.toList());

        return ResponseEntity.ok(
                ApiRes.success(responses,
                        String.format("구글 폼 %d의 %s 상태 지원자 %d명을 조회했습니다.",
                                googleFormId, status, responses.size()))
        );
    }
}