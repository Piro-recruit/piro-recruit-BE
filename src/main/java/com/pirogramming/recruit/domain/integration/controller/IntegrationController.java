package com.pirogramming.recruit.domain.integration.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.pirogramming.recruit.domain.integration.service.AppsScriptIntegrationService;
import com.pirogramming.recruit.global.exception.ApiRes;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/integration")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Integration", description = "외부 시스템 연동 API (Apps Script, CSV 내보내기)")
@PreAuthorize("hasRole('ROOT') or hasRole('GENERAL')")
public class IntegrationController {

    private final AppsScriptIntegrationService appsScriptIntegrationService;

    // 지원자 CSV 내보내기
    // 홈페이지 업데이트용 CSV 파일 생성
    @GetMapping("/export/applicants/csv")
    @Operation(summary = "지원자 CSV 내보내기",
            description = "홈페이지 업데이트용 지원자 CSV 파일을 생성합니다. (name,phone,level,major,is_passed)")
    public ResponseEntity<String> exportApplicantsCsv(
            @Parameter(description = "구글 폼 ID (선택사항, 없으면 전체 지원자)")
            @RequestParam(required = false) Long googleFormId) {

        log.info("지원자 CSV 내보내기 요청 - 구글폼 ID: {}", googleFormId);

        String csvContent = appsScriptIntegrationService.generateApplicantCsv(googleFormId);

        String filename = googleFormId != null ?
                String.format("applicants_form_%d.csv", googleFormId) :
                "applicants_all.csv";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filename + "\"")
                .header(HttpHeaders.CONTENT_TYPE, "text/csv; charset=UTF-8")
                .body(csvContent);
    }

    /**
     * Admin 코드 CSV 내보내기
     * 평가자 배포용 CSV 파일 생성
     */
    @GetMapping("/export/admins/csv")
    @Operation(summary = "Admin 코드 CSV 내보내기",
            description = "평가자 배포용 Admin 로그인 코드 CSV 파일을 생성합니다. (loginCode,identifierName,expiredAt)")
    public ResponseEntity<String> exportAdminsCsv() {

        log.info("Admin 코드 CSV 내보내기 요청");

        String csvContent = appsScriptIntegrationService.generateAdminCsv();

        String filename = String.format("admin_codes_%s.csv",
                java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")));

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filename + "\"")
                .header(HttpHeaders.CONTENT_TYPE, "text/csv; charset=UTF-8")
                .body(csvContent);
    }

    /**
     * 지원자 CSV 미리보기
     */
    @GetMapping("/preview/applicants")
    @Operation(summary = "지원자 CSV 미리보기",
            description = "CSV 내보내기 전 데이터를 미리 확인할 수 있습니다.")
    public ResponseEntity<ApiRes<String>> previewApplicantsCsv(
            @RequestParam(required = false) Long googleFormId,
            @Parameter(description = "미리보기 행 수 (기본 10개)")
            @RequestParam(defaultValue = "10") int limit) {

        String preview = appsScriptIntegrationService.previewApplicantCsv(googleFormId, limit);

        return ResponseEntity.ok(
                ApiRes.success(preview, "지원자 CSV 미리보기가 생성되었습니다.")
        );
    }

    /**
     * Admin 코드 CSV 미리보기
     */
    @GetMapping("/preview/admins")
    @Operation(summary = "Admin 코드 CSV 미리보기",
            description = "Admin 코드 CSV 내보내기 전 데이터를 미리 확인할 수 있습니다.")
    public ResponseEntity<ApiRes<String>> previewAdminsCsv(
            @Parameter(description = "미리보기 행 수 (기본 10개)")
            @RequestParam(defaultValue = "10") int limit) {

        String preview = appsScriptIntegrationService.previewAdminCsv(limit);

        return ResponseEntity.ok(
                ApiRes.success(preview, "Admin 코드 CSV 미리보기가 생성되었습니다.")
        );
    }

    /**
     * CSV 통계 정보
     */
    @GetMapping("/export/statistics")
    @Operation(summary = "CSV 내보내기 통계",
            description = "내보낼 데이터의 통계 정보를 제공합니다.")
    public ResponseEntity<ApiRes<Object>> getCsvStatistics(
            @RequestParam(required = false) Long googleFormId) {

        Object statistics = appsScriptIntegrationService.getCsvExportStatistics(googleFormId);

        return ResponseEntity.ok(
                ApiRes.success(statistics, "CSV 내보내기 통계 정보입니다.")
        );
    }

    /**
     * Apps Script 연결 테스트
     */
    @GetMapping("/test/connection")
    @Operation(summary = "Apps Script 연결 테스트",
            description = "Apps Script와의 연결 상태를 테스트합니다.")
    public ResponseEntity<ApiRes<String>> testConnection() {

        boolean isConnected = appsScriptIntegrationService.testAppsScriptConnection();

        if (isConnected) {
            return ResponseEntity.ok(
                    ApiRes.success("연결 성공", "Apps Script와의 연결이 정상입니다.")
            );
        } else {
            return ResponseEntity.ok(
                    ApiRes.success("연결 실패", "Apps Script 연결에 문제가 있습니다.")
            );
        }
    }
}