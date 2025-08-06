package com.pirogramming.recruit.domain.mail.dto;

import com.pirogramming.recruit.domain.mail.entity.MailSubscriber;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

public class MailSubscriberDto {

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreateRequest {
        @NotBlank(message = "이메일은 필수입니다")
        @Email(message = "올바른 이메일 형식이 아닙니다")
        private String email;

        public MailSubscriber toEntity() {
            return MailSubscriber.builder()
                    .email(email)
                    .build();
        }
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Response {
        private String email;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public static Response from(MailSubscriber mailSubscriber) {
            return Response.builder()
                    .email(mailSubscriber.getEmail())
                    .createdAt(mailSubscriber.getCreatedAt())
                    .updatedAt(mailSubscriber.getUpdatedAt())
                    .build();
        }
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UpdateRequest {
        @NotBlank(message = "이메일은 필수입니다")
        @Email(message = "올바른 이메일 형식이 아닙니다")
        private String email;
    }
}