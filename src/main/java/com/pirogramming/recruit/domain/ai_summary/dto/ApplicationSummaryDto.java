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
    
    @Schema(description = "지원자의 전반적인 요약", example = "3년차 백엔드 개발자로 Java Spring Boot와 MSA 아키텍처 경험을 보유하고 있습니다. 대용량 트래픽 처리 경험이 있으며 성능 최적화에 강점을 보입니다.")
    private String overallSummary;
    
    @Schema(description = "핵심 강점 목록", example = "[\"대용량 트래픽 처리 경험\", \"성능 최적화 능력\", \"MSA 아키텍처 구축 경험\"]")
    private List<String> keyStrengths;
    
    @Schema(description = "기술 스택 목록", example = "[\"Java\", \"Spring Boot\", \"Docker\", \"Redis\", \"PostgreSQL\", \"AWS\"]")
    private List<String> technicalSkills;
    
    @Schema(description = "경력 요약", example = "ABC회사에서 전자상거래 플랫폼 개발 및 결제 시스템 구축, XYZ스타트업에서 관리자 시스템 개발 경험이 있습니다.")
    private String experience;
    
    @Schema(description = "지원동기 요약", example = "대용량 트래픽 처리와 MSA 환경 경험을 바탕으로 팀의 기술적 성장에 기여하고 싶습니다.")
    private String motivation;
    
    @Schema(description = "100점 만점 평가 점수", example = "85", minimum = "0", maximum = "100")
    private int scoreOutOf100;
}