package com.pirogramming.recruit.domain.mail.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pirogramming.recruit.domain.mail.entity.MailSubscriber;
import com.pirogramming.recruit.domain.mail.repository.MailSubscriberRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionCleanupService {

    private final MailSubscriberRepository mailSubscriberRepository;

    // 매일 새벽 2시에 오래된 구독자 정리
    // 12개월 이상 지난 구독자는 자동으로 삭제
    @Scheduled(cron = "0 0 2 * * ?") // 매일 새벽 2시
    @Transactional
    public void cleanupOldSubscriptions() {
        LocalDateTime cutoffDate = LocalDateTime.now().minusMonths(12);

        try {
            List<MailSubscriber> oldSubscribers = mailSubscriberRepository
                    .findByCreatedAtBefore(cutoffDate);

            if (!oldSubscribers.isEmpty()) {
                mailSubscriberRepository.deleteAll(oldSubscribers);
                log.info("구독자 정리 완료 - 삭제된 구독자 수: {} 명 (기준일: {})",
                        oldSubscribers.size(), cutoffDate.toLocalDate());

                // 삭제된 구독자 이메일 로그 (개인정보 보호를 위해 마스킹)
                oldSubscribers.forEach(subscriber ->
                        log.debug("삭제된 구독자: {}", maskEmail(subscriber.getEmail())));
            } else {
                log.info("구독자 정리 - 삭제할 대상이 없습니다");
            }

        } catch (Exception e) {
            log.error("구독자 정리 중 오류 발생: {}", e.getMessage(), e);
        }
    }

    // 수동으로 구독자 정리 실행
    @Transactional
    public int cleanupOldSubscriptionsManually(int months) {
        if (months < 1 || months > 24) {
            throw new IllegalArgumentException("정리 기간은 1~24개월 사이여야 합니다");
        }

        LocalDateTime cutoffDate = LocalDateTime.now().minusMonths(months);
        List<MailSubscriber> oldSubscribers = mailSubscriberRepository
                .findByCreatedAtBefore(cutoffDate);

        if (!oldSubscribers.isEmpty()) {
            mailSubscriberRepository.deleteAll(oldSubscribers);
            log.info("수동 구독자 정리 완료 - 삭제된 구독자 수: {} 명 (기준: {}개월 전)",
                    oldSubscribers.size(), months);
        }

        return oldSubscribers.size();
    }

    // 잘못된 이메일 형식의 구독자 정리
    @Transactional
    public int cleanupInvalidEmails() {
        List<MailSubscriber> allSubscribers = mailSubscriberRepository.findAll();
        List<MailSubscriber> invalidSubscribers = allSubscribers.stream()
                .filter(subscriber -> !isValidEmail(subscriber.getEmail()))
                .toList();

        if (!invalidSubscribers.isEmpty()) {
            mailSubscriberRepository.deleteAll(invalidSubscribers);
            log.info("잘못된 이메일 구독자 정리 완료 - 삭제된 구독자 수: {} 명",
                    invalidSubscribers.size());
        }

        return invalidSubscribers.size();
    }

    // 중복 구독자 정리
    @Transactional
    public int cleanupDuplicateSubscribers() {
        List<String> duplicateEmails = mailSubscriberRepository.findDuplicateEmails();
        int deletedCount = 0;

        for (String email : duplicateEmails) {
            List<MailSubscriber> duplicates = mailSubscriberRepository.findByEmailOrderByCreatedAtAsc(email);
            // 가장 오래된 것 하나만 남기고 나머지 삭제
            if (duplicates.size() > 1) {
                List<MailSubscriber> toDelete = duplicates.subList(1, duplicates.size());
                mailSubscriberRepository.deleteAll(toDelete);
                deletedCount += toDelete.size();
            }
        }

        if (deletedCount > 0) {
            log.info("중복 구독자 정리 완료 - 삭제된 구독자 수: {} 명", deletedCount);
        }

        return deletedCount;
    }

    // 구독자 통계 정보 조회
    public SubscriptionStats getSubscriptionStats() {
        long totalCount = mailSubscriberRepository.count();
        long recentCount = mailSubscriberRepository.countByCreatedAtAfter(
                LocalDateTime.now().minusMonths(3));
        long oldCount = mailSubscriberRepository.countByCreatedAtBefore(
                LocalDateTime.now().minusMonths(12));

        return SubscriptionStats.builder()
                .totalSubscribers(totalCount)
                .recentSubscribers(recentCount)
                .oldSubscribers(oldCount)
                .build();
    }

    // 이메일 유효성 검사
    private boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }

        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
        return email.matches(emailRegex);
    }

    // 이메일 마스킹 (개인정보 보호)
    private String maskEmail(String email) {
        if (email == null || email.isEmpty()) {
            return email;
        }

        int atIndex = email.indexOf('@');
        if (atIndex <= 1) {
            return email;
        }

        String localPart = email.substring(0, atIndex);
        String domainPart = email.substring(atIndex);

        if (localPart.length() <= 2) {
            return email;
        }

        return localPart.charAt(0) + "*".repeat(localPart.length() - 2) +
                localPart.charAt(localPart.length() - 1) + domainPart;
    }

    // 구독자 통계 데이터 클래스
    @lombok.Builder
    @lombok.Getter
    public static class SubscriptionStats {
        private final long totalSubscribers;
        private final long recentSubscribers;  // 최근 3개월
        private final long oldSubscribers;     // 12개월 이상
    }
}