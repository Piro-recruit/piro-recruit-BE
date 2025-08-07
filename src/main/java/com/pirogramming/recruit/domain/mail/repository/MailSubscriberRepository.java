package com.pirogramming.recruit.domain.mail.repository;

import com.pirogramming.recruit.domain.mail.entity.MailSubscriber;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Repository
public interface MailSubscriberRepository extends JpaRepository<MailSubscriber, String> {
    Page<MailSubscriber> findByEmailContaining(String email, Pageable pageable);
    boolean existsByEmail(String email);
}