package com.pirogramming.recruit.domain.webhook.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.pirogramming.recruit.domain.ai_summary.service.ApplicationSummaryService;
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

    // 구글 폼에서 전송된 지원서 데이터를 저장
//    @Transactional
//    public WebhookApplicationResponse processWebhookApplication(WebhookApplicationRequest request) {
//        log.info("웹훅 지원서 처리 시작 - 폼ID: {}, 이메일: {}, 구글폼 응답ID: {}",
//                request.getFormId(), request.getApplicantEmail(), request.getFormResponseId());
//
//        try {
//            // 1. 구글 폼 조회
//            GoogleForm googleForm = googleFormService.getGoogleFormByFormIdRequired(request.getFormId());
//
//            // 2. 중복 검사
//            validateDuplication(request, googleForm);
//
//            // 3. 엔티티 생성 및 저장
//            WebhookApplication application = request.toEntity(googleForm);
//            WebhookApplication savedApplication = webhookApplicationRepository.save(application);
//
//            // 4. 처리 완료 상태 업데이트
//            savedApplication.markAsProcessed();
//
//            log.info("웹훅 지원서 처리 완료 - ID: {}, 이메일: {}",
//                    savedApplication.getId(), savedApplication.getApplicantEmail());
//
//            return WebhookApplicationResponse.from(savedApplication);
//
//        } catch (DuplicateResourceException e) {
//            // 중복 예외는 그대로 던짐
//            log.warn("웹훅 지원서 중복 - 이메일: {}, 오류: {}", request.getApplicantEmail(), e.getMessage());
//            throw e;
//
//        } catch (Exception e) {
//            log.error("웹훅 지원서 처리 실패 - 이메일: {}, 오류: {}", request.getApplicantEmail(), e.getMessage(), e);
//
//            // 실패한 지원서도 일단 저장하고, 실패 상태로 표시
//            try {
//                GoogleForm googleForm = googleFormService.getGoogleFormByFormIdRequired(request.getFormId());
//                WebhookApplication failedApplication = request.toEntity(googleForm);
//                failedApplication.markAsFailed(e.getMessage());
//                WebhookApplication savedFailedApplication = webhookApplicationRepository.save(failedApplication);
//                return WebhookApplicationResponse.from(savedFailedApplication);
//            } catch (Exception saveException) {
//                log.error("실패한 지원서 저장 중 추가 오류 발생: {}", saveException.getMessage(), saveException);
//                throw new RecruitException(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.WEBHOOK_PROCESSING_FAILED);
//            }
//        }
//    }
    @Transactional
    public WebhookApplicationResponse processWebhookApplication(WebhookApplicationRequest request) {
        log.info("웹훅 지원서 처리 시작 - 폼ID: {}, 이메일: {}, 구글폼 응답ID: {}",
                request.getFormId(), request.getApplicantEmail(), request.getFormResponseId());

        try {
            // 1) 구글 폼 존재 확인
            GoogleForm googleForm = googleFormService.getGoogleFormByFormIdRequired(request.getFormId());

            // 2) 중복 검사 (formResponseId만 검사: 이메일 중복은 허용)
            validateDuplication(request, googleForm);

            // 3) 지원서 저장
            WebhookApplication application = request.toEntity(googleForm);
            WebhookApplication savedApplication = webhookApplicationRepository.save(application);

            // 4) 처리 완료 상태로 마킹
            savedApplication.markAsProcessed();

            // 5) AI 요약 생성·저장 (복합 유니크: formResponseId + applicantEmail)
            try {
                applicationSummaryService.summarizeAndSaveFromWebhook(
                        request.getFormId(),
                        request.getFormResponseId(),
                        request.getApplicantName(),
                        request.getApplicantEmail(),
                        request.getFormData()
                );
                log.info("요약 생성/저장 완료 - formResponseId: {}, email: {}",
                        request.getFormResponseId(), request.getApplicantEmail());
            } catch (com.pirogramming.recruit.global.exception.RecruitException e) {
                // 이미 같은 (formResponseId, email) 조합이 있으면 409로 들어올 수 있음 → 경고만 남기고 웹훅 저장은 유지
                if (e.getStatus() != null && e.getStatus().value() == HttpStatus.CONFLICT.value()) {
                    log.warn("요약 중복으로 저장 생략 - formResponseId: {}, email: {}",
                            request.getFormResponseId(), request.getApplicantEmail());
                } else {
                    log.error("요약 생성/저장 실패 - {}", e.getMessage(), e);
                }
            } catch (Exception e) {
                log.error("요약 생성/저장 중 알 수 없는 오류 - {}", e.getMessage(), e);
            }

            log.info("웹훅 지원서 처리 완료 - ID: {}, 이메일: {}", savedApplication.getId(), savedApplication.getApplicantEmail());
            return WebhookApplicationResponse.from(savedApplication);

        } catch (com.pirogramming.recruit.global.exception.RecruitException e) {
            log.error("웹훅 지원서 처리 실패 - {}", e.getMessage(), e);
            try {
                // 실패 이력도 남겨두기
                GoogleForm googleForm = googleFormService.getGoogleFormByFormIdRequired(request.getFormId());
                WebhookApplication failedApplication = request.toEntity(googleForm);
                failedApplication.markAsFailed(e.getMessage());
                WebhookApplication savedFailedApplication = webhookApplicationRepository.save(failedApplication);
                return WebhookApplicationResponse.from(savedFailedApplication);
            } catch (Exception saveException) {
                log.error("실패한 지원서 저장 중 추가 오류 발생: {}", saveException.getMessage(), saveException);
                throw new com.pirogramming.recruit.global.exception.RecruitException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        com.pirogramming.recruit.global.exception.code.ErrorCode.WEBHOOK_PROCESSING_FAILED
                );
            }
        } catch (Exception e) {
            log.error("웹훅 지원서 처리 중 알 수 없는 오류 - {}", e.getMessage(), e);
            throw new com.pirogramming.recruit.global.exception.RecruitException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    com.pirogramming.recruit.global.exception.code.ErrorCode.WEBHOOK_PROCESSING_FAILED
            );
        }
    }


    // 중복 검사
//    private void validateDuplication(WebhookApplicationRequest request, GoogleForm googleForm) {
//        // 구글 폼 응답 ID 중복 검사
//        if (webhookApplicationRepository.existsByFormResponseId(request.getFormResponseId())) {
//            throw new DuplicateResourceException(ErrorCode.WEBHOOK_DUPLICATE_FORM_RESPONSE);
//        }
//
//        // 동일 구글 폼에서 이메일 중복 검사
//        if (webhookApplicationRepository.existsByGoogleFormIdAndApplicantEmail(
//                googleForm.getId(), request.getApplicantEmail())) {
//            throw new DuplicateResourceException(ErrorCode.WEBHOOK_DUPLICATE_EMAIL);
//        }
//    }

    // 중복 검사: formResponseId만 중복 불가 (같은 이메일로 여러 지원서는 허용)
    private void validateDuplication(WebhookApplicationRequest request, GoogleForm googleForm) {
        // 구글 폼 응답 ID 중복 검사
        if (webhookApplicationRepository.existsByFormResponseId(request.getFormResponseId())) {
            throw new com.pirogramming.recruit.global.exception.entity_exception.DuplicateResourceException(
                    com.pirogramming.recruit.global.exception.code.ErrorCode.WEBHOOK_DUPLICATE_FORM_RESPONSE
            );
        }
        // ⚠️ 기존 코드에 있던 "구글 폼 + 이메일 중복" / "폼ID + 이메일 중복" 검사는 제거합니다.
    }


    // 전체 지원서 목록 조회
    public List<WebhookApplicationResponse> getAllApplications() {
        return webhookApplicationRepository.findAllOrderByCreatedAtDesc()
                .stream()
                .map(WebhookApplicationResponse::from)
                .collect(Collectors.toList());
    }

    // 구글 폼별 지원서 목록 조회 (구글 폼 ID)
    public List<WebhookApplicationResponse> getApplicationsByGoogleForm(Long googleFormId) {
        return webhookApplicationRepository.findByGoogleFormIdOrderByCreatedAtDesc(googleFormId)
                .stream()
                .map(WebhookApplicationResponse::from)
                .collect(Collectors.toList());
    }

    // 구글 폼별 지원서 목록 조회 (폼 ID)
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

    // 구글 폼별 지원서 개수 조회
    public long getApplicationCountByGoogleForm(Long googleFormId) {
        return webhookApplicationRepository.countByGoogleFormId(googleFormId);
    }

    // 폼 ID별 지원서 개수 조회 (최적화됨)
    public long getApplicationCountByFormId(String formId) {
        return webhookApplicationRepository.countByFormId(formId);
    }

    // 지원서 제출 여부 확인 (이메일 기준)
    public boolean isApplicationSubmitted(String email) {
        return webhookApplicationRepository.existsByApplicantEmail(email);
    }

    // 구글 폼별 지원서 제출 여부 확인
    public boolean isApplicationSubmittedForGoogleForm(Long googleFormId, String email) {
        return webhookApplicationRepository.existsByGoogleFormIdAndApplicantEmail(googleFormId, email);
    }

    // 폼 ID별 지원서 제출 여부 확인
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

//    summary_ai 추가
    @Transactional
    public WebhookApplicationResponse receiveAndSummarize(WebhookApplicationRequest req) {

        applicationSummaryService.summarizeAndSaveFromWebhook(
                req.getFormId(),
                req.getFormResponseId(),
                req.getApplicantName(),
                req.getApplicantEmail(),
                req.getFormData()
        );

        // 저장 자체는 요약 테이블에 하므로, 컨트롤러 응답은 간단 상태만 내려도 됨.
        return WebhookApplicationResponse.builder()
                .id(null)
                .googleFormId(null)
                .formId(req.getFormId())
                .formTitle(null)
                .applicantName(req.getApplicantName())
                .applicantEmail(req.getApplicantEmail())
                .formResponseId(req.getFormResponseId())
                .submissionTimestamp(req.getSubmissionTimestamp())
                .formData(req.getFormData())
                .status("RECEIVED")
                .errorMessage(null)
                .aiAnalysis(null)
                .createdAt(null)
                .updatedAt(null)
                .build();
    }


}