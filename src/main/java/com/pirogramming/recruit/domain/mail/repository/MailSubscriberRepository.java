package com.pirogramming.recruit.domain.mail.repository;

import com.pirogramming.recruit.domain.mail.entity.MailSubscriber;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MailSubscriberRepository extends JpaRepository<MailSubscriber, String> {
    Page<MailSubscriber> findByEmailContaining(String email, Pageable pageable);
    boolean existsByEmail(String email);

    // 모든 구독자 이메일 목록 조회 (메일 발송용)
    @Query("SELECT m.email FROM MailSubscriber m")
    List<String> findAllEmails();

    // 생성일 기준 조회 (구독자 정리용)
    List<MailSubscriber> findByCreatedAtBefore(LocalDateTime dateTime);
    List<MailSubscriber> findByCreatedAtAfter(LocalDateTime dateTime);
    List<MailSubscriber> findByEmailOrderByCreatedAtAsc(String email);

    // 생성일 기준 카운트 (통계용)
    long countByCreatedAtAfter(LocalDateTime dateTime);
    long countByCreatedAtBefore(LocalDateTime dateTime);

    // 중복 이메일 조회
    @Query("SELECT m.email FROM MailSubscriber m GROUP BY m.email HAVING COUNT(m.email) > 1")
    List<String> findDuplicateEmails();

    // 최근 구독자 조회 (관리용)
    @Query("SELECT m FROM MailSubscriber m WHERE m.createdAt >= :since ORDER BY m.createdAt DESC")
    List<MailSubscriber> findRecentSubscribers(LocalDateTime since);

    // 이메일 패턴 검색
    @Query("SELECT m FROM MailSubscriber m WHERE m.email LIKE %:pattern%")
    List<MailSubscriber> findByEmailPattern(String pattern);
}