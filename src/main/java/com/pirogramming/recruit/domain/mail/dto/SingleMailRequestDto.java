package com.pirogramming.recruit.domain.mail.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class SingleMailRequestDto {
    
    @NotBlank(message = "수신자 이메일은 필수입니다")
    @Email(message = "올바른 이메일 형식이 아닙니다")
    private String recipientEmail;
    
    @NotBlank(message = "제목은 필수입니다")
    private String subject;
    
    @NotBlank(message = "내용은 필수입니다")
    private String content;
}