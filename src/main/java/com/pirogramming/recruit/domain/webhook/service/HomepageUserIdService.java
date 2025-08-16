package com.pirogramming.recruit.domain.webhook.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pirogramming.recruit.domain.webhook.entity.WebhookApplication;
import com.pirogramming.recruit.domain.webhook.repository.WebhookApplicationRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class HomepageUserIdService {

    private final WebhookApplicationRepository webhookApplicationRepository;

    // 새 지원자에게 홈페이지용 순차 ID 할당
    // 홈페이지에서 사용하는 user ID와 매핑하기 위한 순차적 ID (1,2,3...)
    @Transactional
    public Long assignHomepageUserId(WebhookApplication application) {
        if (application.getHomepageUserId() != null) {
            log.debug("이미 홈페이지 User ID가 할당됨: {}", application.getHomepageUserId());
            return application.getHomepageUserId(); // 이미 할당됨
        }

        // 현재 최대 홈페이지 ID 조회
        Long maxId = webhookApplicationRepository.findMaxHomepageUserId().orElse(0L);
        Long newId = maxId + 1;

        // ID 할당
        application.setHomepageUserId(newId);
        webhookApplicationRepository.save(application);

        log.info("새 홈페이지 User ID 할당: {} -> {}", application.getApplicantEmail(), newId);
        return newId;
    }

    // 홈페이지 User ID로 지원서 조회
    public WebhookApplication findByHomepageUserId(Long homepageUserId) {
        return webhookApplicationRepository.findByHomepageUserId(homepageUserId)
                .orElse(null);
    }

    // 현재 할당된 최대 홈페이지 User ID 조회
    public Long getMaxHomepageUserId() {
        return webhookApplicationRepository.findMaxHomepageUserId().orElse(0L);
    }

    // 모든 지원서에 홈페이지 User ID 일괄 할당
    // 기존 데이터 마이그레이션용
    @Transactional
    public void assignAllHomepageUserIds() {
        log.info("모든 지원서에 홈페이지 User ID 일괄 할당 시작");

        var applications = webhookApplicationRepository.findAllOrderByCreatedAtDesc();

        Long currentId = 1L;
        for (WebhookApplication app : applications) {
            if (app.getHomepageUserId() == null) {
                app.setHomepageUserId(currentId++);
                webhookApplicationRepository.save(app);
            }
        }

        log.info("홈페이지 User ID 일괄 할당 완료 - 총 {} 건", currentId - 1);
    }
}