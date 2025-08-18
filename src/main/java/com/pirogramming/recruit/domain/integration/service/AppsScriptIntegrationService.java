package com.pirogramming.recruit.domain.integration.service;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

import com.pirogramming.recruit.domain.admin.dto.GeneralAdminResponse;
import com.pirogramming.recruit.domain.admin.service.AdminService;
import com.pirogramming.recruit.domain.webhook.entity.WebhookApplication;
import com.pirogramming.recruit.domain.webhook.repository.WebhookApplicationRepository;
import com.pirogramming.recruit.domain.webhook.service.HomepageUserIdService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AppsScriptIntegrationService {

    private final WebhookApplicationRepository webhookApplicationRepository;
    private final AdminService adminService;
    private final HomepageUserIdService homepageUserIdService;

    private static final DateTimeFormatter CSV_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // 지원자 CSV 생성 (홈페이지 형식)
    // 형식: name,phone,level,major,is_passed
    public String generateApplicantCsv(Long googleFormId) {
        log.info("지원자 CSV 생성 시작 - 구글폼 ID: {}", googleFormId);

        List<WebhookApplication> applications;

        if (googleFormId != null) {
            applications = webhookApplicationRepository.findByGoogleFormIdWithGoogleFormOrderByCreatedAtDesc(googleFormId);
        } else {
            applications = webhookApplicationRepository.findAllWithGoogleFormOrderByCreatedAtDesc();
        }

        StringBuilder csv = new StringBuilder();
        csv.append("name,phone,level,major,is_passed\n");

        for (WebhookApplication app : applications) {
            // 홈페이지 User ID가 없으면 자동 할당
            if (app.getHomepageUserId() == null) {
                homepageUserIdService.assignHomepageUserId(app);
            }

            String name = escapeCSV(app.getApplicantName());
            String phone = escapeCSV(extractPhoneFromFormData(app));

            int level = app.getGoogleForm().getGeneration();
            int major = determineMajorStatus(app);
            int isPassed = app.getPassStatus().getCsvValue();

            csv.append(String.format("%s,%s,%d,%d,%d\n",
                    name, phone, level, major, isPassed));
        }

        log.info("지원자 CSV 생성 완료 - {} 건", applications.size());
        return csv.toString();
    }

    // Admin 코드 CSV 생성 (평가자 배포용)
    // 형식: loginCode,identifierName,expiredAt
    public String generateAdminCsv() {
        log.info("Admin 코드 CSV 생성 시작");

        List<GeneralAdminResponse> admins = adminService.getAllGeneralAdmins();

        StringBuilder csv = new StringBuilder();
        csv.append("loginCode,identifierName,expiredAt\n");

        for (GeneralAdminResponse admin : admins) {
            String loginCode = escapeCSV(admin.getLoginCode());
            String identifierName = escapeCSV(admin.getIdentifierName());
            String expiredAt = admin.getExpiredAt() != null ?
                    admin.getExpiredAt().format(CSV_DATE_FORMAT) : "";

            csv.append(String.format("%s,%s,%s\n",
                    loginCode, identifierName, expiredAt));
        }

        log.info("Admin 코드 CSV 생성 완료 - {} 건", admins.size());
        return csv.toString();
    }

    // 지원자 CSV 미리보기 생성
    public String previewApplicantCsv(Long googleFormId, int limit) {
        List<WebhookApplication> applications;

        if (googleFormId != null) {
            // 수정: JOIN FETCH 사용
            applications = webhookApplicationRepository.findByGoogleFormIdWithGoogleFormOrderByCreatedAtDesc(googleFormId)
                    .stream().limit(limit).collect(Collectors.toList());
        } else {
            // 수정: JOIN FETCH 사용
            applications = webhookApplicationRepository.findAllWithGoogleFormOrderByCreatedAtDesc()
                    .stream().limit(limit).collect(Collectors.toList());
        }

        StringBuilder preview = new StringBuilder();
        preview.append("name,phone,level,major,is_passed\n");

        for (WebhookApplication app : applications) {
            // 홈페이지 User ID가 없으면 자동 할당
            if (app.getHomepageUserId() == null) {
                homepageUserIdService.assignHomepageUserId(app);
            }

            String name = escapeCSV(app.getApplicantName());
            String phone = escapeCSV(extractPhoneFromFormData(app));
            int level = app.getGoogleForm().getGeneration();
            int major = determineMajorStatus(app);
            int isPassed = app.getPassStatus().getCsvValue();

            preview.append(String.format("%s,%s,%d,%d,%d\n",
                    name, phone, level, major, isPassed));
        }

        return preview.toString();
    }

    // Admin 코드 CSV 미리보기 생성
    public String previewAdminCsv(int limit) {
        List<GeneralAdminResponse> admins = adminService.getAllGeneralAdmins()
                .stream().limit(limit).collect(Collectors.toList());

        StringBuilder preview = new StringBuilder();
        preview.append("loginCode,identifierName,expiredAt\n");

        for (GeneralAdminResponse admin : admins) {
            String loginCode = escapeCSV(admin.getLoginCode());
            String identifierName = escapeCSV(admin.getIdentifierName());
            String expiredAt = admin.getExpiredAt() != null ?
                    admin.getExpiredAt().format(CSV_DATE_FORMAT) : "";

            preview.append(String.format("%s,%s,%s\n",
                    loginCode, identifierName, expiredAt));
        }

        return preview.toString();
    }

    // CSV 내보내기 통계 정보
    public Map<String, Object> getCsvExportStatistics(Long googleFormId) {
        Map<String, Object> stats = new HashMap<>();

        if (googleFormId != null) {
            long applicantCount = webhookApplicationRepository.countByGoogleFormId(googleFormId);

            stats.put("googleFormId", googleFormId);
            stats.put("applicantCount", applicantCount);

            List<WebhookApplication> sampleApps = webhookApplicationRepository.findByGoogleFormIdOrderByCreatedAtDesc(googleFormId);
            if (!sampleApps.isEmpty()) {
                stats.put("recruitmentLevel", sampleApps.get(0).getGoogleForm().getGeneration());
            }

            // 구글 폼별 합격 상태 통계
            Map<WebhookApplication.PassStatus, Long> passStats = new HashMap<>();
            for (WebhookApplication.PassStatus status : WebhookApplication.PassStatus.values()) {
                List<WebhookApplication> statusApps = webhookApplicationRepository
                        .findByGoogleFormIdAndPassStatus(googleFormId, status);
                passStats.put(status, (long) statusApps.size());
            }
            stats.put("passStatusStatistics", passStats);

        } else {
            List<WebhookApplication> allApplications = webhookApplicationRepository.findAllOrderByCreatedAtDesc();
            long totalApplicants = allApplications.size();

            stats.put("totalApplicantCount", totalApplicants);

            Map<Integer, Long> generationStats = allApplications.stream()
                    .collect(Collectors.groupingBy(
                            app -> app.getGoogleForm().getGeneration(),
                            Collectors.counting()
                    ));
            stats.put("generationStatistics", generationStats);

            // 전체 합격 상태 통계
            Map<WebhookApplication.PassStatus, Long> passStats = new HashMap<>();
            for (WebhookApplication.PassStatus status : WebhookApplication.PassStatus.values()) {
                long count = webhookApplicationRepository.countByPassStatus(status);
                passStats.put(status, count);
            }
            stats.put("passStatusStatistics", passStats);
        }

        // Admin 통계 추가
        List<GeneralAdminResponse> admins = adminService.getAllGeneralAdmins();
        long totalAdmins = admins.size();
        long activeAdmins = admins.stream()
                .filter(admin -> admin.getExpiredAt() == null ||
                        admin.getExpiredAt().isAfter(java.time.LocalDateTime.now()))
                .count();

        stats.put("totalAdminCount", totalAdmins);
        stats.put("activeAdminCount", activeAdmins);
        stats.put("exportTimestamp", java.time.LocalDateTime.now());

        return stats;
    }

    // Apps Script 연결 테스트
    public boolean testAppsScriptConnection() {
        // 실제 구현에서는 Apps Script API를 호출하여 연결 상태 확인
        // 여기서는 간단히 true 반환
        log.info("Apps Script 연결 테스트 수행");
        return true;
    }

    // 유틸리티 메서드들

    // formData에서 전화번호 추출
    // 다양한 필드명으로 전화번호를 검색
//    private String extractPhoneFromFormData(WebhookApplication app) {
//        Object phone = app.getFormDataValue("전화번호");
//        if (phone == null) phone = app.getFormDataValue("연락처");
//        if (phone == null) phone = app.getFormDataValue("휴대폰번호");
//        if (phone == null) phone = app.getFormDataValue("phone");
//        if (phone == null) phone = app.getFormDataValue("mobile");
//        if (phone == null) phone = app.getFormDataValue("핸드폰");
//        if (phone == null) phone = app.getFormDataValue("휴대폰");
//        if (phone == null) phone = app.getFormDataValue("연락처 번호");
//
//        String phoneStr = phone != null ? phone.toString().trim() : "";
//
//        // 전화번호 형식 정리 (하이픈 추가)
//        if (!phoneStr.isEmpty() && !phoneStr.contains("-")) {
//            phoneStr = formatPhoneNumber(phoneStr);
//        }
//
//        return phoneStr;
//    }

    private String extractPhoneFromFormData(WebhookApplication app) {
        // 1) 엔티티 필드 우선 (WebhookApplication.phoneNumber)
        String direct = app.getPhoneNumber();
        if (direct != null && !(direct = direct.trim()).isEmpty()) {
            if (!direct.contains("-")) {
                direct = formatPhoneNumber(direct);
            }
            return direct;
        }

        // 2) formData의 흔한 키들 탐색
        Object phone = app.getFormDataValue("전화번호");
        if (phone == null) phone = app.getFormDataValue("연락처");
        if (phone == null) phone = app.getFormDataValue("휴대폰번호");
        if (phone == null) phone = app.getFormDataValue("핸드폰 번호");
        if (phone == null) phone = app.getFormDataValue("휴대폰 번호");
        if (phone == null) phone = app.getFormDataValue("연락처 번호");
        if (phone == null) phone = app.getFormDataValue("핸드폰");
        if (phone == null) phone = app.getFormDataValue("휴대폰");
        if (phone == null) phone = app.getFormDataValue("phone");
        if (phone == null) phone = app.getFormDataValue("mobile");

        String phoneStr = phone != null ? phone.toString().trim() : "";

        // 3) 여전히 비어있다면 formData 전체에서 전화번호 패턴(10~11자리) 탐색
        if ((phoneStr == null || phoneStr.isEmpty()) && app.getFormData() != null) {
            for (Map.Entry<String, Object> entry : app.getFormData().entrySet()) {
                if (entry.getValue() == null) continue;
                String v = entry.getValue().toString();
                String digits = v.replaceAll("[^0-9]", "");
                if (digits.length() == 10 || digits.length() == 11) {
                    phoneStr = v.trim();
                    break;
                }
            }
        }

        // 4) 전화번호 형식 정리 (하이픈 추가)
        if (phoneStr != null && !phoneStr.isEmpty() && !phoneStr.contains("-")) {
            phoneStr = formatPhoneNumber(phoneStr);
        }

        return phoneStr == null ? "" : phoneStr;
    }

    // 전공 여부 판단 (0:비전공, 1:전공, 2:복수전공)
    private int determineMajorStatus(WebhookApplication app) {
        // formData에서 전공 정보 추출
        Object major = app.getFormDataValue("전공");
        if (major == null) major = app.getFormDataValue("학과");
        if (major == null) major = app.getFormDataValue("학부");
        if (major == null) major = app.getFormDataValue("major");
        if (major == null) major = app.getFormDataValue("전공과목");
        if (major == null) major = app.getFormDataValue("소속학과");

        if (major == null) return 0; // 정보 없음 = 비전공으로 처리

        String majorStr = major.toString().toLowerCase();

        // 복수전공 키워드 체크
        if (majorStr.contains("복수") || majorStr.contains("double") ||
                majorStr.contains("이중") || majorStr.contains("부전공")) {
            return 2;
        }

        // 전공 키워드 체크 (IT 관련 전공)
        if (majorStr.contains("컴퓨터") || majorStr.contains("computer") ||
                majorStr.contains("소프트웨어") || majorStr.contains("software") ||
                majorStr.contains("정보") || majorStr.contains("information") ||
                majorStr.contains("전산") || majorStr.contains("데이터") ||
                majorStr.contains("ai") || majorStr.contains("인공지능") ||
                majorStr.contains("프로그래밍") || majorStr.contains("programming") ||
                majorStr.contains("개발") || majorStr.contains("development") ||
                majorStr.contains("it") || majorStr.contains("공학")) {
            return 1; // 전공자
        }

        return 0; // 비전공자
    }

    // 전화번호 형식 정리
    // 010-1234-5678 형식으로 변환
    private String formatPhoneNumber(String phone) {
        // 숫자만 추출
        String digits = phone.replaceAll("[^0-9]", "");

        // 010-1234-5678 형식으로 변환
        if (digits.length() == 11 && digits.startsWith("010")) {
            return digits.substring(0, 3) + "-" +
                    digits.substring(3, 7) + "-" +
                    digits.substring(7);
        }

        // 기타 형식은 원본 반환
        return phone;
    }

    // CSV 값 이스케이프 처리
    // 쉼표, 따옴표, 개행문자 등을 안전하게 처리
    private String escapeCSV(String value) {
        if (value == null) return "";

        value = value.trim();

        // 쉼표, 따옴표, 개행 문자가 있으면 따옴표로 감싸기
        if (value.contains(",") || value.contains("\"") ||
                value.contains("\n") || value.contains("\r")) {
            // 따옴표는 두 개로 이스케이프
            value = value.replace("\"", "\"\"");
            return "\"" + value + "\"";
        }

        return value;
    }
}