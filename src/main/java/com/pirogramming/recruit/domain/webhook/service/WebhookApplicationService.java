package com.pirogramming.recruit.domain.webhook.service;

import com.pirogramming.recruit.domain.recruitment.entity.Recruitment;
import com.pirogramming.recruit.domain.recruitment.service.RecruitmentService;
import com.pirogramming.recruit.domain.webhook.dto.WebhookApplicationRequest;
import com.pirogramming.recruit.domain.webhook.dto.WebhookApplicationResponse;
import com.pirogramming.recruit.domain.webhook.entity.WebhookApplication;
import com.pirogramming.recruit.domain.webhook.repository.WebhookApplicationRepository;
import com.pirogramming.recruit.global.exception.code.ErrorCode;
import com.pirogramming.recruit.global.exception.entity_exception.DuplicateResourceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class WebhookApplicationService {

    private final WebhookApplicationRepository webhookApplicationRepository;
    private final RecruitmentService recruitmentService;

    // 구글 폼에서 전송된 지원서 데이터를 저장
    @Transactional
    public WebhookApplicationResponse processWebhookApplication(WebhookApplicationRequest request) {
        log.info("웹훅 지원서 처리 시작 - 리크루팅ID: {}, 이메일: {}, 구글폼 응답ID: {}",
                request.getRecruitmentId(), request.getApplicantEmail(), request.getFormResponseId());

        try {
            // 1. 리크루팅 조회
            Recruitment recruitment = recruitmentService.getRecruitmentByIdRequired(request.getRecruitmentId());

            // 2. 중복 검사
            validateDuplication(request, recruitment);

            // 3. 엔티티 생성 및 저장
            WebhookApplication application = request.toEntity(recruitment);
            WebhookApplication savedApplication = webhookApplicationRepository.save(application);

            // 4. 처리 완료 상태 업데이트
            savedApplication.markAsProcessed();

            log.info("웹훅 지원서 처리 완료 - ID: {}, 이메일: {}",
                    savedApplication.getId(), savedApplication.getApplicantEmail());

            return WebhookApplicationResponse.from(savedApplication);

        } catch (DuplicateResourceException e) {
            // 중복 예외는 그대로 던짐
            log.warn("웹훅 지원서 중복 - 이메일: {}, 오류: {}", request.getApplicantEmail(), e.getMessage());
            throw e;

        } catch (Exception e) {
            log.error("웹훅 지원서 처리 실패 - 이메일: {}, 오류: {}", request.getApplicantEmail(), e.getMessage(), e);

            // 실패한 지원서도 일단 저장하고, 실패 상태로 표시
            try {
                Recruitment recruitment = recruitmentService.getRecruitmentByIdRequired(request.getRecruitmentId());
                WebhookApplication failedApplication = request.toEntity(recruitment);
                failedApplication.markAsFailed(e.getMessage());
                WebhookApplication savedFailedApplication = webhookApplicationRepository.save(failedApplication);
                return WebhookApplicationResponse.from(savedFailedApplication);
            } catch (Exception saveException) {
                log.error("실패한 지원서 저장 중 추가 오류 발생: {}", saveException.getMessage(), saveException);
                throw new RuntimeException("웹훅 처리 및 실패 저장에 모두 실패했습니다.", e);
            }
        }
    }

    // 중복 검사
    private void validateDuplication(WebhookApplicationRequest request, Recruitment recruitment) {
        // 구글 폼 응답 ID 중복 검사
        if (webhookApplicationRepository.existsByFormResponseId(request.getFormResponseId())) {
            throw new DuplicateResourceException(ErrorCode.WEBHOOK_DUPLICATE_FORM_RESPONSE);
        }

        // 동일 리크루팅에서 이메일 중복 검사
        if (webhookApplicationRepository.existsByRecruitmentIdAndApplicantNameAndApplicantEmail(
                recruitment.getId(), request.getApplicantName(), request.getApplicantEmail())) {
            throw new DuplicateResourceException(ErrorCode.WEBHOOK_DUPLICATE_EMAIL);
        }
    }

    // 전체 지원서 목록 조회
    public List<WebhookApplicationResponse> getAllApplications() {
        return webhookApplicationRepository.findAllOrderByCreatedAtDesc()
                .stream()
                .map(WebhookApplicationResponse::from)
                .collect(Collectors.toList());
    }

    // 리크루팅별 지원서 목록 조회
    public List<WebhookApplicationResponse> getApplicationsByRecruitment(Long recruitmentId) {
        return webhookApplicationRepository.findByRecruitmentIdOrderByCreatedAtDesc(recruitmentId)
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

    // 리크루팅별 + 이메일로 지원서 조회
    public Optional<WebhookApplicationResponse> getApplicationByRecruitmentAndEmail(Long recruitmentId, String email) {
        return webhookApplicationRepository.findByRecruitmentId(recruitmentId)
                .stream()
                .filter(app -> app.getApplicantEmail().equals(email))
                .findFirst()
                .map(WebhookApplicationResponse::from);
    }

    // 처리 상태별 지원서 조회
    public List<WebhookApplicationResponse> getApplicationsByStatus(WebhookApplication.ProcessingStatus status) {
        return webhookApplicationRepository.findByStatus(status)
                .stream()
                .map(WebhookApplicationResponse::from)
                .collect(Collectors.toList());
    }

    // 리크루팅별 + 상태별 지원서 조회
    public List<WebhookApplicationResponse> getApplicationsByRecruitmentAndStatus(Long recruitmentId, WebhookApplication.ProcessingStatus status) {
        return webhookApplicationRepository.findByRecruitmentIdAndStatus(recruitmentId, status)
                .stream()
                .map(WebhookApplicationResponse::from)
                .collect(Collectors.toList());
    }

    // 대기 중인 지원서 개수 조회
    public long getPendingApplicationCount() {
        return webhookApplicationRepository.countPendingApplications();
    }

    // 리크루팅별 지원서 개수 조회
    public long getApplicationCountByRecruitment(Long recruitmentId) {
        return webhookApplicationRepository.countByRecruitmentId(recruitmentId);
    }

    // 지원서 제출 여부 확인 (이메일 기준)
    public boolean isApplicationSubmitted(String email) {
        return webhookApplicationRepository.existsByApplicantEmail(email);
    }

    // 리크루팅별 지원서 제출 여부 확인
    public boolean isApplicationSubmittedForRecruitment(Long recruitmentId, String email) {
        return webhookApplicationRepository.existsByRecruitmentIdAndApplicantNameAndApplicantEmail(
                recruitmentId, null, email);
    }

    // 상태별 통계 조회
    public Map<WebhookApplication.ProcessingStatus, Long> getStatusStatistics() {
        Map<WebhookApplication.ProcessingStatus, Long> statistics = new HashMap<>();

        for (WebhookApplication.ProcessingStatus status : WebhookApplication.ProcessingStatus.values()) {
            long count = webhookApplicationRepository.countByStatus(status);
            statistics.put(status, count);
        }

        return statistics;
    }

    // 리크루팅별 상태별 통계 조회
    public Map<WebhookApplication.ProcessingStatus, Long> getStatusStatisticsByRecruitment(Long recruitmentId) {
        Map<WebhookApplication.ProcessingStatus, Long> statistics = new HashMap<>();

        for (WebhookApplication.ProcessingStatus status : WebhookApplication.ProcessingStatus.values()) {
            long count = webhookApplicationRepository.findByRecruitmentIdAndStatus(recruitmentId, status).size();
            statistics.put(status, count);
        }

        return statistics;
    }
}