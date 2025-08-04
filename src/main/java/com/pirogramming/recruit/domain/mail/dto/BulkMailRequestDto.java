package com.pirogramming.recruit.domain.mail.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class BulkMailRequestDto {
    
    @NotBlank(message = "제목은 필수입니다")
    private String subject;
    
    @NotBlank(message = "내용은 필수입니다")
    private String content;
}