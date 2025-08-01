package com.pirogramming.recruit.domain.recruitment.controller;

import com.pirogramming.recruit.domain.recruitment.dto.RecruitmentRequest;
import com.pirogramming.recruit.domain.recruitment.dto.RecruitmentResponse;
import com.pirogramming.recruit.domain.recruitment.entity.Recruitment;
import com.pirogramming.recruit.domain.recruitment.service.RecruitmentService;
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
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/recruitments")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Recruitment", description = "리크루팅 관리 API")
public class RecruitmentController {

    private final RecruitmentService recruitmentService;
    private final WebhookApplicationService webhookApplicationService;

    // 새 리크루팅 생성
    @PostMapping
    @Operation(summary = "새 리크루팅 생성", description = "새로운 리크루팅을 생성합니다.")
    public ResponseEntity<ApiRes<RecruitmentResponse>> createRecruitment(
            @Valid @RequestBody RecruitmentRequest request) {

        log.info("새 리크루팅 생성 요청 - 제목: {}", request.getTitle());

        Recruitment recruitment = recruitmentService.createRecruitment(request.toEntity());
        RecruitmentResponse response = RecruitmentResponse.from(recruitment);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiRes.success(response, "리크루팅이 성공적으로 생성되었습니다."));
    }

    // 전체 리크루팅 목록 조회
    @GetMapping
    @Operation(summary = "전체 리크루팅 조회", description = "모든 리크루팅을 최신순으로 조회합니다.")
    public ResponseEntity<ApiRes<List<RecruitmentResponse>>> getAllRecruitments(
            @Parameter(description = "지원서 개수 포함 여부") @RequestParam(defaultValue = "false") boolean includeApplicationCount) {

        List<Recruitment> recruitments = recruitmentService.getAllRecruitments();

        List<RecruitmentResponse> responses;
        if (includeApplicationCount) {
            responses = recruitments.stream()
                    .map(recruitment -> {
                        long count = webhookApplicationService.getApplicationCountByRecruitment(recruitment.getId());
                        return RecruitmentResponse.fromWithApplicationCount(recruitment, count);
                    })
                    .collect(Collectors.toList());
        } else {
            responses = recruitments.stream()
                    .map(RecruitmentResponse::from)
                    .collect(Collectors.toList());
        }

        return ResponseEntity.ok(
                ApiRes.success(responses, responses.size() + "개의 리크루팅을 조회했습니다.")
        );
    }

    // 특정 리크루팅 조회
    @GetMapping("/{id}")
    @Operation(summary = "특정 리크루팅 조회", description = "ID를 기준으로 특정 리크루팅을 조회합니다.")
    public ResponseEntity<ApiRes<RecruitmentResponse>> getRecruitmentById(
            @Parameter(description = "리크루팅 ID") @PathVariable Long id,
            @Parameter(description = "지원서 개수 포함 여부") @RequestParam(defaultValue = "true") boolean includeApplicationCount) {

        return recruitmentService.getRecruitmentById(id)
                .map(recruitment -> {
                    RecruitmentResponse response;
                    if (includeApplicationCount) {
                        long count = webhookApplicationService.getApplicationCountByRecruitment(recruitment.getId());
                        response = RecruitmentResponse.fromWithApplicationCount(recruitment, count);
                    } else {
                        response = RecruitmentResponse.from(recruitment);
                    }
                    return ResponseEntity.ok(ApiRes.success(response));
                })
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiRes.failure(HttpStatus.NOT_FOUND, ErrorCode.RECRUITMENT_NOT_FOUND)));
    }

    // 현재 모집 중인 리크루팅 조회
    @GetMapping("/active")
    @Operation(summary = "현재 모집 중인 리크루팅 조회", description = "현재 모집 중인 리크루팅을 조회합니다.")
    public ResponseEntity<ApiRes<RecruitmentResponse>> getCurrentActiveRecruitment() {

        return recruitmentService.getCurrentActiveRecruitment()
                .map(recruitment -> {
                    long count = webhookApplicationService.getApplicationCountByRecruitment(recruitment.getId());
                    RecruitmentResponse response = RecruitmentResponse.fromWithApplicationCount(recruitment, count);
                    return ResponseEntity.ok(ApiRes.success(response));
                })
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiRes.failure(HttpStatus.NOT_FOUND, ErrorCode.RECRUITMENT_NOT_ACTIVE)));
    }

    // 특정 기간의 리크루팅들 조회
    @GetMapping("/application-period")
    @Operation(summary = "특정 기간의 리크루팅 조회", description = "특정 지원 기간에 해당하는 리크루팅들을 조회합니다.")
    public ResponseEntity<ApiRes<List<RecruitmentResponse>>> getActiveRecruitmentsByDate() {

        List<Recruitment> recruitments = recruitmentService.getActiveRecruitmentsByDate();
        List<RecruitmentResponse> responses = recruitments.stream()
                .map(recruitment -> {
                    long count = webhookApplicationService.getApplicationCountByRecruitment(recruitment.getId());
                    return RecruitmentResponse.fromWithApplicationCount(recruitment, count);
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(
                ApiRes.success(responses, "특정 지원 기간인 리크루팅 " + responses.size() + "개를 조회했습니다.")
        );
    }

    // 상태별 리크루팅 조회
    @GetMapping("/by-status")
    @Operation(summary = "상태별 리크루팅 조회", description = "상태를 기준으로 리크루팅을 조회합니다.")
    public ResponseEntity<ApiRes<List<RecruitmentResponse>>> getRecruitmentsByStatus(
            @Parameter(description = "리크루팅 상태 (DRAFT, ACTIVE, COMPLETED, CANCELLED)")
            @RequestParam Recruitment.RecruitmentStatus status) {

        List<Recruitment> recruitments = recruitmentService.getRecruitmentsByStatus(status);
        List<RecruitmentResponse> responses = recruitments.stream()
                .map(RecruitmentResponse::from)
                .collect(Collectors.toList());

        return ResponseEntity.ok(
                ApiRes.success(responses, status + " 상태의 리크루팅 " + responses.size() + "개를 조회했습니다.")
        );
    }

    // 리크루팅 활성화
    @PutMapping("/{id}/activate")
    @Operation(summary = "리크루팅 활성화", description = "특정 리크루팅을 활성화합니다. (기존 활성화된 것은 비활성화)")
    public ResponseEntity<ApiRes<RecruitmentResponse>> activateRecruitment(
            @Parameter(description = "리크루팅 ID") @PathVariable Long id) {

        log.info("리크루팅 활성화 요청 - ID: {}", id);

        Recruitment recruitment = recruitmentService.activateRecruitment(id);
        long count = webhookApplicationService.getApplicationCountByRecruitment(recruitment.getId());
        RecruitmentResponse response = RecruitmentResponse.fromWithApplicationCount(recruitment, count);

        return ResponseEntity.ok(
                ApiRes.success(response, "리크루팅이 활성화되었습니다.")
        );
    }

    // 리크루팅 완료 처리
    @PutMapping("/{id}/complete")
    @Operation(summary = "리크루팅 완료 처리", description = "특정 리크루팅을 완료 상태로 변경합니다.")
    public ResponseEntity<ApiRes<RecruitmentResponse>> completeRecruitment(
            @Parameter(description = "리크루팅 ID") @PathVariable Long id) {

        log.info("리크루팅 완료 처리 요청 - ID: {}", id);

        Recruitment recruitment = recruitmentService.completeRecruitment(id);
        long count = webhookApplicationService.getApplicationCountByRecruitment(recruitment.getId());
        RecruitmentResponse response = RecruitmentResponse.fromWithApplicationCount(recruitment, count);

        return ResponseEntity.ok(
                ApiRes.success(response, "리크루팅이 완료 처리되었습니다.")
        );
    }

    // 구글 폼 URL 업데이트
    @PutMapping("/{id}/google-form-url")
    @Operation(summary = "구글 폼 URL 업데이트", description = "리크루팅의 구글 폼 URL을 업데이트합니다.")
    public ResponseEntity<ApiRes<RecruitmentResponse>> updateGoogleFormUrl(
            @Parameter(description = "리크루팅 ID") @PathVariable Long id,
            @RequestBody Map<String, String> request) {

        String newUrl = request.get("googleFormUrl");
        if (newUrl == null || newUrl.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiRes.failure(HttpStatus.BAD_REQUEST, ErrorCode.INVALID_ARGUMENT));
        }

        log.info("구글 폼 URL 업데이트 요청 - ID: {}, URL: {}", id, newUrl);

        Recruitment recruitment = recruitmentService.updateGoogleFormUrl(id, newUrl);
        RecruitmentResponse response = RecruitmentResponse.from(recruitment);

        return ResponseEntity.ok(
                ApiRes.success(response, "구글 폼 URL이 업데이트되었습니다.")
        );
    }

    // 리크루팅 통계 정보 조회
    @GetMapping("/{id}/statistics")
    @Operation(summary = "리크루팅 통계 정보", description = "특정 리크루팅의 상세 통계 정보를 조회합니다.")
    public ResponseEntity<ApiRes<Map<String, Object>>> getRecruitmentStatistics(
            @Parameter(description = "리크루팅 ID") @PathVariable Long id) {

        // 리크루팅 존재 확인
        Recruitment recruitment = recruitmentService.getRecruitmentByIdRequired(id);

        // 통계 정보 수집
        long totalApplications = webhookApplicationService.getApplicationCountByRecruitment(id);
        Map<com.pirogramming.recruit.domain.webhook.entity.WebhookApplication.ProcessingStatus, Long> statusStatistics =
                webhookApplicationService.getStatusStatisticsByRecruitment(id);

        Map<String, Object> statistics = Map.of(
                "recruitmentId", id,
                "recruitmentTitle", recruitment.getTitle(),
                "totalApplications", totalApplications,
                "statusStatistics", statusStatistics,
                "isActive", recruitment.getIsActive(),
                "isApplicationPeriod", recruitment.isApplicationPeriod(),
                "startDate", recruitment.getStartDate(),
                "endDate", recruitment.getEndDate()
        );

        return ResponseEntity.ok(
                ApiRes.success(statistics, "리크루팅 통계 정보를 조회했습니다.")
        );
    }
}