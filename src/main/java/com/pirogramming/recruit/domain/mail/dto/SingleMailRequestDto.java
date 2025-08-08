package com.pirogramming.recruit.domain.mail.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "단일 메일 발송 요청 DTO")
public class SingleMailRequestDto {
    
    @NotBlank(message = "수신자 이메일은 필수입니다")
    @Email(message = "올바른 이메일 형식이 아닙니다")
    @Schema(description = "수신자 이메일 주소", example = "recipient@example.com")
    private String recipientEmail;
    
    @NotBlank(message = "제목은 필수입니다")
    @Schema(description = "메일 제목", example = "피로그래밍 지원서 안내")
    private String subject;
    
    @NotBlank(message = "내용은 필수입니다")
    @Schema(description = "메일 내용 (HTML 형식 지원)", example = "<h1>안녕하세요</h1><p>지원서 접수가 완료되었습니다.</p>")
    private String content;
}