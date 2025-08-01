package com.pirogramming.recruit.domain.webhook.dto;

import com.pirogramming.recruit.domain.webhook.entity.WebhookApplication;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class WebhookApplicationRequest {

    @NotBlank(message = "이름은 필수입니다")
    @Size(min = 2, max = 50, message = "이름은 2자 이상 50자 이하여야 합니다")
    private String name;

    @NotBlank(message = "이메일은 필수입니다")
    @Email(message = "올바른 이메일 형식이 아닙니다")
    private String email;

    @NotBlank(message = "전화번호는 필수입니다")
    @Size(min = 10, max = 15, message = "전화번호는 10자 이상 15자 이하여야 합니다")
    private String phone;

    @NotBlank(message = "학교는 필수입니다")
    @Size(max = 100, message = "학교명은 100자 이하여야 합니다")
    private String school;

    @NotBlank(message = "전공은 필수입니다")
    @Size(max = 100, message = "전공명은 100자 이하여야 합니다")
    private String major;

    private String portfolioUrl;

    @NotBlank(message = "자기소개는 필수입니다")
    @Size(min = 50, max = 2000, message = "자기소개는 50자 이상 2000자 이하여야 합니다")
    private String introduction;

    @NotBlank(message = "지원동기는 필수입니다")
    @Size(min = 50, max = 2000, message = "지원동기는 50자 이상 2000자 이하여야 합니다")
    private String motivation;

    @NotBlank(message = "구글 폼 응답 ID는 필수입니다")
    private String formResponseId;

    @NotBlank(message = "제출 시간은 필수입니다")
    private String submissionTimestamp;

    // DTO를 Entity로 변환
    public WebhookApplication toEntity() {
        return WebhookApplication.builder()
                .name(this.name)
                .email(this.email)
                .phone(this.phone)
                .school(this.school)
                .major(this.major)
                .portfolioUrl(this.portfolioUrl)
                .introduction(this.introduction)
                .motivation(this.motivation)
                .formResponseId(this.formResponseId)
                .submissionTimestamp(this.submissionTimestamp)
                .build();
    }
}