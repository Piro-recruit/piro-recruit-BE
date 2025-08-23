package com.pirogramming.recruit.domain.googleform.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.pirogramming.recruit.domain.googleform.dto.GoogleFormRequest;
import com.pirogramming.recruit.domain.googleform.dto.GoogleFormResponse;
import com.pirogramming.recruit.domain.googleform.entity.GoogleForm;
import com.pirogramming.recruit.domain.googleform.entity.FormStatus;
import com.pirogramming.recruit.domain.googleform.service.GoogleFormService;
import com.pirogramming.recruit.domain.webhook.entity.WebhookApplication;
import com.pirogramming.recruit.domain.webhook.service.WebhookApplicationService;
import com.pirogramming.recruit.global.exception.ApiRes;
import com.pirogramming.recruit.global.exception.code.ErrorCode;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.pirogramming.recruit.global.security.RequireRoot;
import com.pirogramming.recruit.global.security.RequireAdmin;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/google-forms")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "GoogleForm", description = "구글 폼 관리 API")
public class GoogleFormController {

    private final GoogleFormService googleFormService;
    private final WebhookApplicationService webhookApplicationService;

    // 공통: 지원서 개수와 함께 응답 생성
    private GoogleFormResponse buildResponseWithApplicationCount(GoogleForm googleForm, Map<Long, Long> applicationCountMap) {
        Long count = applicationCountMap.getOrDefault(googleForm.getId(), 0L);
        return GoogleFormResponse.fromWithApplicationCount(googleForm, count);
    }

    // 공통: 지원서 개수와 함께 응답 생성 (단일)
    private GoogleFormResponse buildResponseWithApplicationCount(GoogleForm googleForm) {
        long count = webhookApplicationService.getApplicationCountByGoogleForm(googleForm.getId());
        return GoogleFormResponse.fromWithApplicationCount(googleForm, count);
    }

    // 새 구글 폼 생성
    @PostMapping
    @RequireRoot
    @Operation(summary = "새 구글 폼 생성", description = "새로운 구글 폼을 등록합니다.")
    public ResponseEntity<ApiRes<GoogleFormResponse>> createGoogleForm(
            @Valid @RequestBody GoogleFormRequest request) {

        log.info("새 구글 폼 생성 요청 - 제목: {}, 폼ID: {}", request.getTitle(), request.getFormId());

        GoogleForm googleForm = googleFormService.createGoogleForm(request.toEntity());
        GoogleFormResponse response = GoogleFormResponse.from(googleForm);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiRes.success(response, "구글 폼이 성공적으로 생성되었습니다."));
    }

    // 전체 구글 폼 목록 조회
    @GetMapping
    @RequireAdmin
    @Operation(summary = "전체 구글 폼 조회", description = "모든 구글 폼을 최신순으로 조회합니다.")
    public ResponseEntity<ApiRes<List<GoogleFormResponse>>> getAllGoogleForms(
            @Parameter(description = "지원서 개수 포함 여부") @RequestParam(defaultValue = "true") boolean includeApplicationCount) {

        List<GoogleForm> googleForms = googleFormService.getAllGoogleForms();

        List<GoogleFormResponse> responses;
        if (includeApplicationCount) {
            // 배치 쿼리로 N+1 문제 해결
            List<Long> googleFormIds = googleForms.stream()
                    .map(GoogleForm::getId)
                    .collect(Collectors.toList());
            Map<Long, Long> applicationCountMap = webhookApplicationService.getApplicationCountsByGoogleForms(googleFormIds);

            responses = googleForms.stream()
                    .map(googleForm -> buildResponseWithApplicationCount(googleForm, applicationCountMap))
                    .collect(Collectors.toList());
        } else {
            responses = googleForms.stream()
                    .map(GoogleFormResponse::from)
                    .collect(Collectors.toList());
        }

        return ResponseEntity.ok(
                ApiRes.success(responses, responses.size() + "개의 구글 폼을 조회했습니다.")
        );
    }

    // 특정 구글 폼 조회 (ID 기준)
    @GetMapping("/{id}")
    @RequireAdmin
    @Operation(summary = "특정 구글 폼 조회", description = "ID를 기준으로 특정 구글 폼을 조회합니다.")
    public ResponseEntity<ApiRes<GoogleFormResponse>> getGoogleFormById(
            @Parameter(description = "구글 폼 ID") @PathVariable Long id,
            @Parameter(description = "지원서 개수 포함 여부") @RequestParam(defaultValue = "true") boolean includeApplicationCount) {

        return googleFormService.getGoogleFormById(id)
                .map(googleForm -> {
                    GoogleFormResponse response = includeApplicationCount ?
                            buildResponseWithApplicationCount(googleForm) :
                            GoogleFormResponse.from(googleForm);
                    return ResponseEntity.ok(ApiRes.success(response));
                })
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiRes.failure(HttpStatus.NOT_FOUND, ErrorCode.NOT_FOUND)));
    }

    // 폼 ID로 구글 폼 조회
    @GetMapping("/form-id/{formId}")
    @RequireAdmin
    @Operation(summary = "폼 ID로 구글 폼 조회", description = "구글 폼 ID를 기준으로 구글 폼을 조회합니다.")
    public ResponseEntity<ApiRes<GoogleFormResponse>> getGoogleFormByFormId(
            @Parameter(description = "구글 폼 ID") @PathVariable String formId,
            @Parameter(description = "지원서 개수 포함 여부") @RequestParam(defaultValue = "true") boolean includeApplicationCount) {

        return googleFormService.getGoogleFormByFormId(formId)
                .map(googleForm -> {
                    GoogleFormResponse response = includeApplicationCount ?
                            buildResponseWithApplicationCount(googleForm) :
                            GoogleFormResponse.from(googleForm);
                    return ResponseEntity.ok(ApiRes.success(response));
                })
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiRes.failure(HttpStatus.NOT_FOUND, ErrorCode.NOT_FOUND)));
    }

    // 현재 활성화된 구글 폼 조회
    @GetMapping("/active")
    @RequireAdmin
    @Operation(summary = "현재 활성화된 구글 폼 조회", description = "현재 활성화된 구글 폼을 조회합니다.")
    public ResponseEntity<ApiRes<GoogleFormResponse>> getActiveGoogleForm() {

        return googleFormService.getActiveGoogleForm()
                .map(googleForm -> {
                    GoogleFormResponse response = buildResponseWithApplicationCount(googleForm);
                    return ResponseEntity.ok(ApiRes.success(response));
                })
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiRes.failure(HttpStatus.NOT_FOUND, ErrorCode.NOT_FOUND)));
    }

    // 활성화된 구글 폼 존재 여부 확인 (인증 불필요)
    @GetMapping("/active/exists")
    @Operation(summary = "활성화된 구글 폼 존재 여부 확인", description = "현재 활성화된 구글 폼이 있는지 여부와 URL을 반환합니다.")
    public ResponseEntity<ApiRes<Map<String, Object>>> checkActiveGoogleFormExists() {

        Optional<GoogleForm> activeGoogleForm = googleFormService.getActiveGoogleForm();
        
        Map<String, Object> result = new HashMap<>();
        String message;
        
        if (activeGoogleForm.isPresent()) {
            GoogleForm googleForm = activeGoogleForm.get();
            result.put("exists", true);
            result.put("formUrl", googleForm.getFormUrl());
            message = "활성화된 구글 폼이 있습니다.";
        } else {
            result.put("exists", false);
            result.put("formUrl", null);
            message = "활성화된 구글 폼이 없습니다.";
        }

        return ResponseEntity.ok(ApiRes.success(result, message));
    }


    // 구글 폼 활성화
    @PutMapping("/{id}/activate")
    @RequireRoot
    @Operation(summary = "구글 폼 활성화", description = "특정 구글 폼을 활성화합니다. (기존 활성화된 것은 비활성화)")
    public ResponseEntity<ApiRes<GoogleFormResponse>> activateGoogleForm(
            @Parameter(description = "구글 폼 ID") @PathVariable Long id) {

        log.info("구글 폼 활성화 요청 - ID: {}", id);

        GoogleForm googleForm = googleFormService.activateGoogleForm(id);
        GoogleFormResponse response = buildResponseWithApplicationCount(googleForm);

        return ResponseEntity.ok(
                ApiRes.success(response, "구글 폼이 활성화되었습니다.")
        );
    }

    // 구글 폼 비활성화
    @PutMapping("/{id}/deactivate")
    @RequireRoot
    @Operation(summary = "구글 폼 비활성화", description = "특정 구글 폼을 비활성화합니다.")
    public ResponseEntity<ApiRes<GoogleFormResponse>> deactivateGoogleForm(
            @Parameter(description = "구글 폼 ID") @PathVariable Long id) {

        log.info("구글 폼 비활성화 요청 - ID: {}", id);

        GoogleForm googleForm = googleFormService.deactivateGoogleForm(id);
        GoogleFormResponse response = buildResponseWithApplicationCount(googleForm);

        return ResponseEntity.ok(
                ApiRes.success(response, "구글 폼이 비활성화되었습니다.")
        );
    }

    // 구글 폼 마감
    @PutMapping("/{id}/close")
    @RequireRoot
    @Operation(summary = "구글 폼 마감", description = "특정 구글 폼을 마감 상태로 변경합니다.")
    public ResponseEntity<ApiRes<GoogleFormResponse>> closeGoogleForm(
            @Parameter(description = "구글 폼 ID") @PathVariable Long id) {

        log.info("구글 폼 마감 요청 - ID: {}", id);

        GoogleForm googleForm = googleFormService.closeGoogleForm(id);
        GoogleFormResponse response = buildResponseWithApplicationCount(googleForm);

        return ResponseEntity.ok(
                ApiRes.success(response, "구글 폼이 마감되었습니다.")
        );
    }

    // 구글 폼 URL 업데이트
    @PutMapping("/{id}/form-url")
    @RequireRoot
    @Operation(summary = "구글 폼 URL 업데이트", description = "구글 폼의 URL을 업데이트합니다.")
    public ResponseEntity<ApiRes<GoogleFormResponse>> updateGoogleFormUrl(
            @Parameter(description = "구글 폼 ID") @PathVariable Long id,
            @RequestBody Map<String, String> request) {

        String newUrl = request.get("formUrl");
        if (newUrl == null || newUrl.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiRes.failure(HttpStatus.BAD_REQUEST, ErrorCode.INVALID_ARGUMENT));
        }

        log.info("구글 폼 URL 업데이트 요청 - ID: {}, URL: {}", id, newUrl);

        GoogleForm googleForm = googleFormService.updateGoogleFormUrl(id, newUrl);
        GoogleFormResponse response = GoogleFormResponse.from(googleForm);

        return ResponseEntity.ok(
                ApiRes.success(response, "구글 폼 URL이 업데이트되었습니다.")
        );
    }

    // 구글 시트 URL 업데이트
    @PutMapping("/{id}/sheet-url")
    @RequireRoot
    @Operation(summary = "구글 시트 URL 업데이트", description = "구글 시트의 URL을 업데이트합니다.")
    public ResponseEntity<ApiRes<GoogleFormResponse>> updateGoogleSheetUrl(
            @Parameter(description = "구글 폼 ID") @PathVariable Long id,
            @RequestBody Map<String, String> request) {

        String newUrl = request.get("sheetUrl");
        if (newUrl == null || newUrl.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiRes.failure(HttpStatus.BAD_REQUEST, ErrorCode.INVALID_ARGUMENT));
        }

        log.info("구글 시트 URL 업데이트 요청 - ID: {}, URL: {}", id, newUrl);

        GoogleForm googleForm = googleFormService.updateGoogleSheetUrl(id, newUrl);
        GoogleFormResponse response = GoogleFormResponse.from(googleForm);

        return ResponseEntity.ok(
                ApiRes.success(response, "구글 시트 URL이 업데이트되었습니다.")
        );
    }

    // 구글 폼 통계 정보 조회
    @GetMapping("/{id}/statistics")
    @RequireAdmin
    @Operation(summary = "구글 폼 통계 정보", description = "특정 구글 폼의 상세 통계 정보를 조회합니다.")
    public ResponseEntity<ApiRes<Map<String, Object>>> getGoogleFormStatistics(
            @Parameter(description = "구글 폼 ID") @PathVariable Long id) {

        // 구글 폼 존재 확인
        GoogleForm googleForm = googleFormService.getGoogleFormByIdRequired(id);

        // 통계 정보 수집
        long totalApplications = webhookApplicationService.getApplicationCountByGoogleForm(id);
        Map<WebhookApplication.ProcessingStatus, Long> statusStatistics =
                webhookApplicationService.getStatusStatisticsByGoogleForm(id);

        Map<String, Object> statistics = Map.of(
                "googleFormId", id,
                "formId", googleForm.getFormId(),
                "formTitle", googleForm.getTitle(),
                "totalApplications", totalApplications,
                "statusStatistics", statusStatistics,
                "status", googleForm.getStatus(),
                "createdAt", googleForm.getCreatedAt()
        );

        return ResponseEntity.ok(
                ApiRes.success(statistics, "구글 폼 통계 정보를 조회했습니다.")
        );
    }

    // 제목으로 구글 폼 검색
    @GetMapping("/search")
    @RequireAdmin
    @Operation(summary = "제목으로 구글 폼 검색", description = "제목을 기준으로 구글 폼을 검색합니다.")
    public ResponseEntity<ApiRes<List<GoogleFormResponse>>> searchGoogleFormsByTitle(
            @Parameter(description = "검색할 제목") @RequestParam String title) {

        List<GoogleForm> googleForms = googleFormService.searchGoogleFormsByTitle(title);
        List<GoogleFormResponse> responses = googleForms.stream()
                .map(GoogleFormResponse::from)
                .collect(Collectors.toList());

        return ResponseEntity.ok(
                ApiRes.success(responses, "'" + title + "'로 검색된 구글 폼 " + responses.size() + "개를 조회했습니다.")
        );
    }

    // 구글 폼 삭제
    @DeleteMapping("/{id}")
    @RequireRoot
    @Operation(summary = "구글 폼 삭제", description = "특정 구글 폼을 삭제합니다. 활성화된 폼은 삭제할 수 없습니다.")
    public ResponseEntity<ApiRes<Void>> deleteGoogleForm(
            @Parameter(description = "구글 폼 ID") @PathVariable Long id) {

        log.info("구글 폼 삭제 요청 - ID: {}", id);

        googleFormService.deleteGoogleForm(id);

        return ResponseEntity.ok(
                ApiRes.success(null, "구글 폼이 성공적으로 삭제되었습니다.")
        );
    }

    // 특정 기수의 구글 폼들 조회
    @GetMapping("/generation/{generation}")
    @RequireAdmin
    @Operation(summary = "특정 기수의 구글 폼들 조회", description = "특정 기수의 모든 구글 폼을 조회합니다.")
    public ResponseEntity<ApiRes<List<GoogleFormResponse>>> getGoogleFormsByGeneration(
            @Parameter(description = "기수") @PathVariable Integer generation,
            @Parameter(description = "지원서 개수 포함 여부") @RequestParam(defaultValue = "true") boolean includeApplicationCount) {

        List<GoogleForm> googleForms = googleFormService.getGoogleFormsByGeneration(generation);

        List<GoogleFormResponse> responses;
        if (includeApplicationCount) {
            List<Long> googleFormIds = googleForms.stream()
                    .map(GoogleForm::getId)
                    .collect(Collectors.toList());
            Map<Long, Long> applicationCountMap = webhookApplicationService.getApplicationCountsByGoogleForms(googleFormIds);

            responses = googleForms.stream()
                    .map(googleForm -> buildResponseWithApplicationCount(googleForm, applicationCountMap))
                    .collect(Collectors.toList());
        } else {
            responses = googleForms.stream()
                    .map(GoogleFormResponse::from)
                    .collect(Collectors.toList());
        }

        return ResponseEntity.ok(
                ApiRes.success(responses, generation + "기 구글 폼 " + responses.size() + "개를 조회했습니다.")
        );
    }

    // 특정 기수의 활성화된 구글 폼 조회
    @GetMapping("/generation/{generation}/active")
    @RequireAdmin
    @Operation(summary = "특정 기수의 활성화된 구글 폼 조회", description = "특정 기수에서 현재 활성화된 구글 폼을 조회합니다.")
    public ResponseEntity<ApiRes<GoogleFormResponse>> getActiveGoogleFormByGeneration(
            @Parameter(description = "기수") @PathVariable Integer generation) {

        return googleFormService.getActiveGoogleFormByGeneration(generation)
                .map(googleForm -> {
                    GoogleFormResponse response = buildResponseWithApplicationCount(googleForm);
                    return ResponseEntity.ok(ApiRes.success(response));
                })
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiRes.failure(HttpStatus.NOT_FOUND, ErrorCode.NOT_FOUND)));
    }

    // 현재 활성화된 기수 조회
    @GetMapping("/current-generation")
    @RequireAdmin
    @Operation(summary = "현재 활성화된 기수 조회", description = "현재 활성화된 구글 폼의 기수를 조회합니다.")
    public ResponseEntity<ApiRes<Map<String, Object>>> getCurrentGeneration() {

        return googleFormService.getCurrentActiveGeneration()
                .map(generation -> {
                    Map<String, Object> result = Map.of(
                            "currentGeneration", generation,
                            "message", "현재 " + generation + "기가 활성화되어 있습니다."
                    );
                    return ResponseEntity.ok(ApiRes.success(result));
                })
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiRes.failure(HttpStatus.NOT_FOUND, ErrorCode.GOOGLE_FORM_NOT_ACTIVE)));
    }

    // 가장 최신 기수 조회
    @GetMapping("/latest-generation")
    @RequireAdmin
    @Operation(summary = "가장 최신 기수 조회", description = "등록된 구글 폼 중 가장 최신 기수를 조회합니다.")
    public ResponseEntity<ApiRes<Map<String, Object>>> getLatestGeneration() {

        return googleFormService.getLatestGeneration()
                .map(generation -> {
                    Map<String, Object> result = Map.of(
                            "latestGeneration", generation,
                            "message", "가장 최신 기수는 " + generation + "기입니다."
                    );
                    return ResponseEntity.ok(ApiRes.success(result));
                })
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiRes.failure(HttpStatus.NOT_FOUND, ErrorCode.NOT_FOUND)));
    }

    // 기수 업데이트
    @PutMapping("/{id}/generation")
    @RequireRoot
    @Operation(summary = "구글 폼 기수 업데이트", description = "구글 폼의 기수를 업데이트합니다.")
    public ResponseEntity<ApiRes<GoogleFormResponse>> updateGoogleFormGeneration(
            @Parameter(description = "구글 폼 ID") @PathVariable Long id,
            @RequestBody Map<String, Integer> request) {

        Integer newGeneration = request.get("generation");
        if (newGeneration == null || newGeneration <= 0) {
            return ResponseEntity.badRequest()
                    .body(ApiRes.failure(HttpStatus.BAD_REQUEST, ErrorCode.INVALID_ARGUMENT));
        }

        log.info("구글 폼 기수 업데이트 요청 - ID: {}, 기수: {}", id, newGeneration);

        GoogleForm googleForm = googleFormService.updateGoogleFormGeneration(id, newGeneration);
        GoogleFormResponse response = GoogleFormResponse.from(googleForm);

        return ResponseEntity.ok(
                ApiRes.success(response, "구글 폼 기수가 " + newGeneration + "기로 업데이트되었습니다.")
        );
    }

    // 기수 범위로 구글 폼 조회
    @GetMapping("/generation-range")
    @RequireAdmin
    @Operation(summary = "기수 범위로 구글 폼 조회", description = "특정 기수 범위의 구글 폼들을 조회합니다.")
    public ResponseEntity<ApiRes<List<GoogleFormResponse>>> getGoogleFormsByGenerationRange(
            @Parameter(description = "시작 기수") @RequestParam Integer startGeneration,
            @Parameter(description = "끝 기수") @RequestParam Integer endGeneration) {

        if (startGeneration > endGeneration) {
            return ResponseEntity.badRequest()
                    .body(ApiRes.failure(HttpStatus.BAD_REQUEST, ErrorCode.INVALID_ARGUMENT));
        }

        List<GoogleForm> googleForms = googleFormService.getGoogleFormsByGenerationRange(startGeneration, endGeneration);
        List<GoogleFormResponse> responses = googleForms.stream()
                .map(GoogleFormResponse::from)
                .collect(Collectors.toList());

        return ResponseEntity.ok(
                ApiRes.success(responses, startGeneration + "기부터 " + endGeneration + "기까지 구글 폼 " + responses.size() + "개를 조회했습니다.")
        );
    }
}