package com.pirogramming.recruit.domain.ai_summary.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "지원서 문항과 답변")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationQuestionDto {
    
    @Schema(description = "지원서 문항", example = "지원분야", required = true)
    private String question;
    
    @Schema(description = "문항에 대한 답변", example = "백엔드 개발자", required = true)
    private String answer;
}