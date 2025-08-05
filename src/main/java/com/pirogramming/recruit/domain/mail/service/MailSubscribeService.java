package com.pirogramming.recruit.domain.mail.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pirogramming.recruit.domain.mail.dto.MailSubscriberDto;
import com.pirogramming.recruit.domain.mail.entity.MailSubscriber;
import com.pirogramming.recruit.domain.mail.repository.MailSubscriberRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MailSubscribeService {

    private final MailSubscriberRepository mailSubscriberRepository;

    @Transactional
    public MailSubscriberDto.Response createSubscriber(MailSubscriberDto.CreateRequest request) {
        if (mailSubscriberRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("이미 구독 중인 이메일입니다");
        }

        MailSubscriber savedSubscriber = mailSubscriberRepository.save(request.toEntity());
        return MailSubscriberDto.Response.from(savedSubscriber);
    }

    public MailSubscriberDto.Response getSubscriber(String email) {
        MailSubscriber subscriber = mailSubscriberRepository.findById(email)
                .orElseThrow(() -> new IllegalArgumentException("해당 이메일의 구독자를 찾을 수 없습니다"));
        return MailSubscriberDto.Response.from(subscriber);
    }

    public Page<MailSubscriberDto.Response> getAllSubscribers(Pageable pageable, String email) {
        if (email != null && !email.trim().isEmpty()) {
            return mailSubscriberRepository.findByEmailContaining(email.trim(), pageable)
                    .map(MailSubscriberDto.Response::from);
        }
        return mailSubscriberRepository.findAll(pageable)
                .map(MailSubscriberDto.Response::from);
    }

    @Transactional
    public MailSubscriberDto.Response updateSubscriber(String currentEmail, MailSubscriberDto.UpdateRequest request) {
        MailSubscriber subscriber = mailSubscriberRepository.findById(currentEmail)
                .orElseThrow(() -> new IllegalArgumentException("해당 이메일의 구독자를 찾을 수 없습니다"));

        if (!currentEmail.equals(request.getEmail()) && mailSubscriberRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다");
        }

        mailSubscriberRepository.delete(subscriber);
        MailSubscriber newSubscriber = MailSubscriber.builder()
                .email(request.getEmail())
                .build();
        MailSubscriber savedSubscriber = mailSubscriberRepository.save(newSubscriber);
        
        return MailSubscriberDto.Response.from(savedSubscriber);
    }

    @Transactional
    public void deleteSubscriber(String email) {
        if (!mailSubscriberRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("해당 이메일의 구독자를 찾을 수 없습니다");
        }
        mailSubscriberRepository.deleteById(email);
    }

    public long getSubscriberCount() {
        return mailSubscriberRepository.count();
    }
}
