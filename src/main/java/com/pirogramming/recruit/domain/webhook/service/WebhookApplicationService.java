package com.pirogramming.recruit.domain.webhook.service;

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

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class WebhookApplicationService {

    private final WebhookApplicationRepository webhookApplicationRepository;

    // 구글 폼에서 전송된 지원서 데이터를 저장
    @Transactional
    public WebhookApplicationResponse processWebhookApplication(WebhookApplicationRequest request) {
        log.info("웹훅 지원서 처리 시작 - 이메일: {}, 구글폼 응답ID: {}",
                request.getEmail(), request.getFormResponseId());

        try {
            // 1. 중복 검사
            validateDuplication(request);

            // 2. 엔티티 생성 및 저장
            WebhookApplication application = request.toEntity();
            WebhookApplication savedApplication = webhookApplicationRepository.save(application);

            // 3. 처리 완료 상태 업데이트
            savedApplication.markAsProcessed();

            log.info("웹훅 지원서 처리 완료 - ID: {}, 이메일: {}",
                    savedApplication.getId(), savedApplication.getEmail());

            return WebhookApplicationResponse.from(savedApplication);

        } catch (DuplicateResourceException e) {
            // 중복 예외는 그대로 처리
            log.warn("웹훅 지원서 중복 - 이메일: {}, 오류: {}", request.getEmail(), e.getMessage());
            throw e;

        } catch (Exception e) {
            log.error("웹훅 지원서 처리 실패 - 이메일: {}, 오류: {}", request.getEmail(), e.getMessage(), e);

            // 실패한 지원서도 일단 저장하고, 실패 상태로 표시
            try {
                WebhookApplication failedApplication = request.toEntity();
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
    private void validateDuplication(WebhookApplicationRequest request) {
        // 구글 폼 응답 ID 중복 검사
        if (webhookApplicationRepository.existsByFormResponseId(request.getFormResponseId())) {
            throw new DuplicateResourceException(ErrorCode.WEBHOOK_DUPLICATE_FORM_RESPONSE);
        }

        // 이메일 중복 검사
        if (webhookApplicationRepository.existsByEmail(request.getEmail())) {
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

    // 특정 지원서 조회 (ID 기준)
    public Optional<WebhookApplicationResponse> getApplicationById(Long id) {
        return webhookApplicationRepository.findById(id)
                .map(WebhookApplicationResponse::from);
    }

    // 이메일로 지원서 조회
    public Optional<WebhookApplicationResponse> getApplicationByEmail(String email) {
        return webhookApplicationRepository.findByEmail(email)
                .map(WebhookApplicationResponse::from);
    }

    // 처리 상태별 지원서 조회
    public List<WebhookApplicationResponse> getApplicationsByStatus(WebhookApplication.ProcessingStatus status) {
        return webhookApplicationRepository.findByStatus(status)
                .stream()
                .map(WebhookApplicationResponse::from)
                .collect(Collectors.toList());
    }

    // 대기 중인 지원서 개수 조회
    public long getPendingApplicationCount() {
        return webhookApplicationRepository.countPendingApplications();
    }

    // 지원서 제출 여부 확인 (이메일 기준)
    public boolean isApplicationSubmitted(String email) {
        return webhookApplicationRepository.existsByEmail(email);
    }
}