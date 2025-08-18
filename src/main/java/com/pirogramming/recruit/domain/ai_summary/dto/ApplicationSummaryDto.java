package com.pirogramming.recruit.domain.ai_summary.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "AI 지원서 요약 결과")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationSummaryDto {
    
    @Schema(description = "각 질문별 AI 요약 결과 목록")
    private List<QuestionSummaryDto> questionSummaries;
    
    @Schema(description = "100점 만점 평가 점수", example = "78", minimum = "0", maximum = "100")
    private int scoreOutOf100;
    
    @Schema(description = "점수 산출 근거 (객관적)", example = "열정 및 학습 태도: 30/40점 - 프로젝트 경험과 학습 계획이 구체적임. 협업 잠재력: 22/30점 - 팀 프로젝트 경험 보유. 기술적 기반: 16/20점 - 다양한 기술 스택 학습 경험. 성장 마인드셋: 8/10점 - 지속적인 개선 의지 확인됨.")
    private String scoreReason;
    
    // 기존 호환성을 위한 생성자 (fallback 용도)
    public ApplicationSummaryDto(String overallSummary, List<String> keyStrengths, 
                                List<String> technicalSkills, String experience, 
                                String motivation, int scoreOutOf100) {
        this.scoreOutOf100 = scoreOutOf100;
        this.scoreReason = "분석 오류로 인해 점수 근거를 제공할 수 없습니다.";
        // 기존 데이터를 questionSummaries로 변환하지 않고 null로 유지
        this.questionSummaries = null;
    }
    
    @Schema(description = "개별 질문에 대한 AI 요약")
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuestionSummaryDto {
        
        @Schema(description = "질문 내용", example = "1. 본인의 가치관, 성격 등을 포함한 자기소개와 피로그래밍에 지원한 동기 및 목표를 적어주세요")
        private String question;
        
        @Schema(description = "AI 요약 결과", example = "컴퓨터공학 전공 학생으로 웹 개발에 대한 관심이 높고, 피로그래밍을 통한 성장 의지를 보여줍니다.")
        private String aiSummary;
    }
}