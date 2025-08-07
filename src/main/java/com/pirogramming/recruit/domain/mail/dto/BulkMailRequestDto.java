package com.pirogramming.recruit.domain.mail.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "일괄 메일 발송 요청 DTO")
public class BulkMailRequestDto {
    
    @NotBlank(message = "제목은 필수입니다")
    @Schema(description = "메일 제목", example = "피로그래밍 모집 공고")
    private String subject;
    
    @NotBlank(message = "내용은 필수입니다")
    @Schema(description = "메일 내용 (HTML 형식 지원)", example = "<h1>피로그래밍 지원 안내</h1><p>지원서를 제출해주세요.</p>")
    private String content;
}