package com.pirogramming.recruit.domain.mail.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "기본 메일 요청 DTO")
public class MailRequestDto {
    
    @NotBlank(message = "제목은 필수입니다")
    @Schema(description = "메일 제목", example = "피로그래밍 관련 안내")
    private String subject;
    
    @NotBlank(message = "내용은 필수입니다")
    @Schema(description = "메일 내용", example = "안녕하세요. 피로그래밍 관련 안내드립니다.")
    private String content;
    
    @Schema(description = "HTML 형식 여부", example = "false", defaultValue = "false")
    private boolean isHtml = false;
}