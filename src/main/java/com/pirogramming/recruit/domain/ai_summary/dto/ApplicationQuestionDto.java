package com.pirogramming.recruit.domain.ai_summary.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "지원서 문항과 답변")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationQuestionDto {
    
    @Schema(description = "지원서 문항", example = "1. 본인의 가치관, 성격 등을 포함한 자기소개", required = true)
    @NotBlank(message = "질문은 필수입니다")
    @Size(max = 500, message = "질문은 최대 500자까지 가능합니다")
    private String question;
    
    @Schema(description = "문항에 대한 답변", example = "안녕하세요. 컴퓨터공학을 전공하고 있는 학생입니다...", required = true)
    @NotBlank(message = "답변은 필수입니다")
    @Size(max = 5000, message = "답변은 최대 5000자까지 가능합니다")
    private String answer;
}