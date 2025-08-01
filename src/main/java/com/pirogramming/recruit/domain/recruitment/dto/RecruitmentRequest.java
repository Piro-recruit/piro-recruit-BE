package com.pirogramming.recruit.domain.recruitment.dto;

import com.pirogramming.recruit.domain.recruitment.entity.Recruitment;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.URL;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class RecruitmentRequest {

    @NotBlank(message = "리크루팅 제목은 필수입니다")
    @Size(min = 2, max = 100, message = "리크루팅 제목은 2자 이상 100자 이하여야 합니다")
    private String title;

    @Size(max = 2000, message = "설명은 2000자 이하여야 합니다")
    private String description;

    @NotNull(message = "지원 시작일은 필수입니다")
    private LocalDate startDate;

    @NotNull(message = "지원 마감일은 필수입니다")
    private LocalDate endDate;

    @NotBlank(message = "구글 폼 URL은 필수입니다")
    @URL(message = "올바른 URL 형식이 아닙니다")
    @Size(max = 1000, message = "URL은 1000자 이하여야 합니다")
    private String googleFormUrl;

    @URL(message = "올바른 URL 형식이 아닙니다")
    @Size(max = 1000, message = "URL은 1000자 이하여야 합니다")
    private String googleSheetUrl;

    // DTO를 Entity로 변환
    public Recruitment toEntity() {
        return Recruitment.builder()
                .title(this.title)
                .description(this.description)
                .startDate(this.startDate)
                .endDate(this.endDate)
                .googleFormUrl(this.googleFormUrl)
                .googleSheetUrl(this.googleSheetUrl)
                .build();
    }
}