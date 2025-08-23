package com.pirogramming.recruit.domain.webhook.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.pirogramming.recruit.domain.ai_summary.service.ApplicationSummaryService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pirogramming.recruit.domain.googleform.entity.GoogleForm;
import com.pirogramming.recruit.domain.googleform.service.GoogleFormService;
import com.pirogramming.recruit.domain.webhook.dto.WebhookApplicationRequest;
import com.pirogramming.recruit.domain.webhook.dto.WebhookApplicationResponse;
import com.pirogramming.recruit.domain.webhook.entity.WebhookApplication;
import com.pirogramming.recruit.domain.webhook.repository.WebhookApplicationRepository;
import com.pirogramming.recruit.global.exception.RecruitException;
import com.pirogramming.recruit.global.exception.code.ErrorCode;
import com.pirogramming.recruit.global.exception.entity_exception.DuplicateResourceException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class WebhookApplicationService {

    private final WebhookApplicationRepository webhookApplicationRepository;
    private final GoogleFormService googleFormService;
    private final ApplicationSummaryService applicationSummaryService;

    /**
     * 구글 폼에서 전송된 지원서 데이터를 저장 + AI 요약 트리거
     * - 중복 정책: formResponseId만 중복 불가 (같은 이메일의 복수 지원 허용)
     * - 저장 성공 후 processed 마킹
     * - 요약 생성/저장은 실패해도 웹훅 저장은 유지 (409(CONFLICT)는 경고 로그만)
     */
    @Transactional
    public WebhookApplicationResponse processWebhookApplication(WebhookApplicationRequest request) {
        log.info("웹훅 지원서 처리 시작 - 폼ID: {}, 이메일: {}, 구글폼 응답ID: {}",
                request.getFormId(), request.getApplicantEmail(), request.getFormResponseId());

        try {
            // 1) 구글 폼 존재 확인
            GoogleForm googleForm = googleFormService.getGoogleFormByFormIdRequired(request.getFormId());

            // 2) 중복 검사: formResponseId만 검사
            validateDuplication(request, googleForm);

            // 3) 엔티티 생성 및 저장
            WebhookApplication application = request.toEntity(googleForm);
            WebhookApplication savedApplication = webhookApplicationRepository.save(application);

            // 4) 처리 완료 상태로 마킹
            savedApplication.markAsProcessed();

            // 5) AI 요약 생성·저장 (WebhookApplication 엔티티와 연결)
            try {
                applicationSummaryService.createPendingSummaryFromWebhook(savedApplication);
                log.info("요약 생성/저장 완료 - applicationId: {}, email: {}",
                        savedApplication.getId(), savedApplication.getApplicantEmail());
            } catch (RecruitException e) {
                // 이미 AI 요약이 있으면 409로 들어올 수 있음 → 경고만 남기고 웹훅 저장은 유지
                if (e.getStatus() != null && e.getStatus().value() == HttpStatus.CONFLICT.value()) {
                    log.warn("요약 중복으로 저장 생략 - applicationId: {}, email: {}",
                            savedApplication.getId(), savedApplication.getApplicantEmail());
                } else {
                    log.error("요약 생성/저장 실패 - {}", e.getMessage(), e);
                }
            } catch (Exception e) {
                log.error("요약 생성/저장 중 알 수 없는 오류 - {}", e.getMessage(), e);
            }

            log.info("웹훅 지원서 처리 완료 - ID: {}, 이메일: {}", savedApplication.getId(), savedApplication.getApplicantEmail());
            return WebhookApplicationResponse.from(savedApplication);

        } catch (RecruitException e) {
            // 도메인 예외의 경우에도 실패 이력은 남겨둔다
            log.error("웹훅 지원서 처리 실패 - {}", e.getMessage(), e);
            try {
                GoogleForm googleForm = googleFormService.getGoogleFormByFormIdRequired(request.getFormId());
                WebhookApplication failedApplication = request.toEntity(googleForm);
                failedApplication.markAsFailed(e.getMessage());
                WebhookApplication savedFailedApplication = webhookApplicationRepository.save(failedApplication);
                return WebhookApplicationResponse.from(savedFailedApplication);
            } catch (Exception saveException) {
                log.error("실패한 지원서 저장 중 추가 오류 발생: {}", saveException.getMessage(), saveException);
                throw new RecruitException(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.WEBHOOK_PROCESSING_FAILED);
            }
        } catch (Exception e) {
            log.error("웹훅 지원서 처리 중 알 수 없는 오류 - {}", e.getMessage(), e);
            throw new RecruitException(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.WEBHOOK_PROCESSING_FAILED);
        }
    }

    /**
     * 중복 검사: formResponseId만 중복 불가 (같은 이메일로 여러 지원서는 허용)
     */
    private void validateDuplication(WebhookApplicationRequest request, GoogleForm googleForm) {
        if (webhookApplicationRepository.existsByFormResponseId(request.getFormResponseId())) {
            throw new DuplicateResourceException(ErrorCode.WEBHOOK_DUPLICATE_FORM_RESPONSE);
        }
        // ⚠️ 기존 "구글 폼 + 이메일 중복" 검사는 제거함
    }

    // ========================= 조회/통계 공용 =========================

    // 전체 지원서 목록 조회
    public List<WebhookApplicationResponse> getAllApplications() {
        return webhookApplicationRepository.findAllOrderByCreatedAtDesc()
                .stream()
                .map(WebhookApplicationResponse::from)
                .collect(Collectors.toList());
    }

    // 구글 폼별 지원서 목록 조회 (구글 폼 PK)
    public List<WebhookApplicationResponse> getApplicationsByGoogleForm(Long googleFormId) {
        return webhookApplicationRepository.findByGoogleFormIdOrderByCreatedAtDesc(googleFormId)
                .stream()
                .map(WebhookApplicationResponse::from)
                .collect(Collectors.toList());
    }

    // 구글 폼별 지원서 목록 조회 (외부 formId)
    public List<WebhookApplicationResponse> getApplicationsByFormId(String formId) {
        return webhookApplicationRepository.findByFormIdOrderByCreatedAtDesc(formId)
                .stream()
                .map(WebhookApplicationResponse::from)
                .collect(Collectors.toList());
    }

    // 특정 지원서 조회 (ID 기준)
    public Optional<WebhookApplicationResponse> getApplicationById(Long id) {
        return webhookApplicationRepository.findById(id)
                .map(WebhookApplicationResponse::from);
    }

    // 이메일로 지원서 조회
    public Optional<WebhookApplicationResponse> getApplicationByEmail(String email) {
        return webhookApplicationRepository.findByApplicantEmail(email)
                .map(WebhookApplicationResponse::from);
    }

    // 구글 폼별 + 이메일로 지원서 조회 (최적화됨)
    public Optional<WebhookApplicationResponse> getApplicationByGoogleFormAndEmail(Long googleFormId, String email) {
        return webhookApplicationRepository.findByGoogleFormIdAndApplicantEmail(googleFormId, email)
                .map(WebhookApplicationResponse::from);
    }

    // 폼 ID + 이메일로 지원서 조회 (최적화됨)
    public Optional<WebhookApplicationResponse> getApplicationByFormIdAndEmail(String formId, String email) {
        return webhookApplicationRepository.findByFormIdAndApplicantEmail(formId, email)
                .map(WebhookApplicationResponse::from);
    }

    // 처리 상태별 지원서 조회
    public List<WebhookApplicationResponse> getApplicationsByStatus(WebhookApplication.ProcessingStatus status) {
        return webhookApplicationRepository.findByStatus(status)
                .stream()
                .map(WebhookApplicationResponse::from)
                .collect(Collectors.toList());
    }

    // 구글 폼별 + 상태별 지원서 조회
    public List<WebhookApplicationResponse> getApplicationsByGoogleFormAndStatus(Long googleFormId, WebhookApplication.ProcessingStatus status) {
        return webhookApplicationRepository.findByGoogleFormIdAndStatus(googleFormId, status)
                .stream()
                .map(WebhookApplicationResponse::from)
                .collect(Collectors.toList());
    }

    // 대기 중인 지원서 개수 조회
    public long getPendingApplicationCount() {
        return webhookApplicationRepository.countPendingApplications();
    }

    // 구글 폼별 지원서 개수 조회 (구글 폼 PK)
    public long getApplicationCountByGoogleForm(Long googleFormId) {
        return webhookApplicationRepository.countByGoogleFormId(googleFormId);
    }

    // 폼 ID별 지원서 개수 조회 (외부 formId)
    public long getApplicationCountByFormId(String formId) {
        return webhookApplicationRepository.countByFormId(formId);
    }

    // 지원서 제출 여부 확인 (이메일 기준)
    public boolean isApplicationSubmitted(String email) {
        return webhookApplicationRepository.existsByApplicantEmail(email);
    }

    // 구글 폼별 지원서 제출 여부 확인 (구글 폼 PK)
    public boolean isApplicationSubmittedForGoogleForm(Long googleFormId, String email) {
        return webhookApplicationRepository.existsByGoogleFormIdAndApplicantEmail(googleFormId, email);
    }

    // 폼 ID별 지원서 제출 여부 확인 (외부 formId)
    public boolean isApplicationSubmittedForFormId(String formId, String email) {
        return webhookApplicationRepository.existsByFormIdAndApplicantEmail(formId, email);
    }

    // 상태별 통계 조회 (최적화됨)
    public Map<WebhookApplication.ProcessingStatus, Long> getStatusStatistics() {
        return calculateStatusStatistics(status -> webhookApplicationRepository.countByStatus(status));
    }

    // 구글 폼별 상태별 통계 조회 (최적화됨)
    public Map<WebhookApplication.ProcessingStatus, Long> getStatusStatisticsByGoogleForm(Long googleFormId) {
        return calculateStatusStatistics(status -> webhookApplicationRepository.countByGoogleFormIdAndStatus(googleFormId, status));
    }

    // 여러 구글 폼의 지원서 개수를 한번에 조회 (N+1 방지)
    public Map<Long, Long> getApplicationCountsByGoogleForms(List<Long> googleFormIds) {
        if (googleFormIds == null || googleFormIds.isEmpty()) {
            return new HashMap<>();
        }

        List<Object[]> results = webhookApplicationRepository.countByGoogleFormIds(googleFormIds);
        Map<Long, Long> countMap = new HashMap<>();

        // 쿼리 결과를 Map으로 변환
        for (Object[] result : results) {
            Long googleFormId = (Long) result[0];
            Long count = (Long) result[1];
            countMap.put(googleFormId, count);
        }

        // 조회된 결과에 없는 구글 폼은 0으로 설정
        for (Long googleFormId : googleFormIds) {
            countMap.putIfAbsent(googleFormId, 0L);
        }

        return countMap;
    }

    // 통계 계산 공통 로직 (중복 제거)
    private Map<WebhookApplication.ProcessingStatus, Long> calculateStatusStatistics(java.util.function.Function<WebhookApplication.ProcessingStatus, Long> countFunction) {
        Map<WebhookApplication.ProcessingStatus, Long> statistics = new HashMap<>();

        for (WebhookApplication.ProcessingStatus status : WebhookApplication.ProcessingStatus.values()) {
            long count = countFunction.apply(status);
            statistics.put(status, count);
        }

        return statistics;
    }

    // ========================= 합격 상태 관련 (원래 코드 유지) =========================

    // ID로 지원서 조회 (필수) -> 없으면 예외 발생
    public WebhookApplication getApplicationByIdRequired(Long id) {
        return webhookApplicationRepository.findById(id)
                .orElseThrow(() -> new RecruitException(HttpStatus.NOT_FOUND, ErrorCode.WEBHOOK_APPLICATION_NOT_FOUND));
    }

    // 합격 상태 변경 (개별)
    @Transactional
    public WebhookApplication updatePassStatus(Long id, WebhookApplication.PassStatus passStatus) {
        WebhookApplication application = getApplicationByIdRequired(id);

        switch (passStatus) {
            case FIRST_PASS -> application.markAsFirstPass();
            case FINAL_PASS -> application.markAsFinalPass();
            case FAILED -> application.markAsPassFailed();
            case PENDING -> application.resetPassStatus();
        }

        WebhookApplication saved = webhookApplicationRepository.save(application);
        log.info("합격 상태 변경: {} -> {}", application.getApplicantEmail(), passStatus);

        return saved;
    }

    // 합격 상태 일괄 변경
    @Transactional
    public List<WebhookApplication> updatePassStatusAll(List<Long> ids, WebhookApplication.PassStatus passStatus) {
        List<WebhookApplication> applications = webhookApplicationRepository.findAllById(ids);

        for (WebhookApplication app : applications) {
            switch (passStatus) {
                case FIRST_PASS -> app.markAsFirstPass();
                case FINAL_PASS -> app.markAsFinalPass();
                case FAILED -> app.markAsPassFailed();
                case PENDING -> app.resetPassStatus();
            }
        }

        List<WebhookApplication> saved = webhookApplicationRepository.saveAll(applications);
        log.info("일괄 합격 상태 변경: {} 건 -> {}", applications.size(), passStatus);

        return saved;
    }


    // 구글 폼별 점수 상위 N명 합격 상태 변경
    @Transactional
    public List<WebhookApplication> updatePassStatusForTopNByGoogleForm(Long googleFormId, Integer topN, WebhookApplication.PassStatus passStatus) {
        Pageable pageable = PageRequest.of(0, topN);
        List<WebhookApplication> topApplications = webhookApplicationRepository.findTopByAverageScoreAndGoogleForm(googleFormId, pageable);
        
        if (topApplications.isEmpty()) {
            log.warn("구글 폼 {}에 평가된 지원서가 없습니다.", googleFormId);
            return List.of();
        }

        for (WebhookApplication app : topApplications) {
            switch (passStatus) {
                case FIRST_PASS -> app.markAsFirstPass();
                case FINAL_PASS -> app.markAsFinalPass();
                case FAILED -> app.markAsPassFailed();
                case PENDING -> app.resetPassStatus();
            }
        }

        List<WebhookApplication> saved = webhookApplicationRepository.saveAll(topApplications);
        log.info("구글 폼 {} 점수 상위 {} 명 합격 상태 변경: {} -> {}", googleFormId, topN, topApplications.size(), passStatus);

        return saved;
    }

    // 구글 폼별 점수 하위 N명 합격 상태 변경
    @Transactional
    public List<WebhookApplication> updatePassStatusForBottomNByGoogleForm(Long googleFormId, Integer bottomN, WebhookApplication.PassStatus passStatus) {
        Pageable pageable = PageRequest.of(0, bottomN);
        List<WebhookApplication> bottomApplications = webhookApplicationRepository.findBottomByAverageScoreAndGoogleForm(googleFormId, pageable);
        
        if (bottomApplications.isEmpty()) {
            log.warn("구글 폼 {}에 평가된 지원서가 없습니다.", googleFormId);
            return List.of();
        }

        for (WebhookApplication app : bottomApplications) {
            switch (passStatus) {
                case FIRST_PASS -> app.markAsFirstPass();
                case FINAL_PASS -> app.markAsFinalPass();
                case FAILED -> app.markAsPassFailed();
                case PENDING -> app.resetPassStatus();
            }
        }

        List<WebhookApplication> saved = webhookApplicationRepository.saveAll(bottomApplications);
        log.info("구글 폼 {} 점수 하위 {} 명 합격 상태 변경: {} -> {}", googleFormId, bottomN, bottomApplications.size(), passStatus);

        return saved;
    }

    // 합격 상태별 지원서 조회
    public List<WebhookApplication> getApplicationsByPassStatus(WebhookApplication.PassStatus passStatus) {
        return webhookApplicationRepository.findByPassStatus(passStatus);
    }

    // 합격 상태별 개수 조회
    public long getApplicationCountByPassStatus(WebhookApplication.PassStatus passStatus) {
        return webhookApplicationRepository.countByPassStatus(passStatus);
    }

    // 구글 폼별 + 합격 상태별 지원서 조회
    public List<WebhookApplication> getApplicationsByGoogleFormAndPassStatus(Long googleFormId, WebhookApplication.PassStatus passStatus) {
        return webhookApplicationRepository.findByGoogleFormIdAndPassStatus(googleFormId, passStatus);
    }

    // 합격 상태 통계 조회
    public Map<WebhookApplication.PassStatus, Long> getPassStatusStatistics() {
        Map<WebhookApplication.PassStatus, Long> statistics = new HashMap<>();

        for (WebhookApplication.PassStatus status : WebhookApplication.PassStatus.values()) {
            long count = getApplicationCountByPassStatus(status);
            statistics.put(status, count);
        }

        return statistics;
    }

    // 구글 폼별 합격 상태 통계 조회
    public Map<WebhookApplication.PassStatus, Long> getPassStatusStatisticsByGoogleForm(Long googleFormId) {
        Map<WebhookApplication.PassStatus, Long> statistics = new HashMap<>();

        for (WebhookApplication.PassStatus status : WebhookApplication.PassStatus.values()) {
            List<WebhookApplication> applications = getApplicationsByGoogleFormAndPassStatus(googleFormId, status);
            statistics.put(status, (long) applications.size());
        }

        return statistics;
    }

    // ========================= summary_ai 전용 엔드포인트 =========================

    /**
     * 기존 WebhookApplication에 대해 AI 요약 생성 (수동 트리거용)
     */
    @Transactional
    public WebhookApplication generateSummaryForExistingApplication(Long applicationId) {
        WebhookApplication application = getApplicationByIdRequired(applicationId);
        
        // AI 요약 생성 (동기식 - 테스트/즉시처리용)
        applicationSummaryService.summarizeAndSaveFromWebhookSync(application);
        
        return application;
    }
}
