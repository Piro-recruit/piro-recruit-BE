package com.pirogramming.recruit.domain.webhook.dto;

import java.time.LocalDateTime;
import java.util.Map;

import com.pirogramming.recruit.domain.googleform.entity.GoogleForm;
import com.pirogramming.recruit.domain.webhook.entity.WebhookApplication;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class WebhookApplicationRequest {

    @NotBlank(message = "구글 폼 ID는 필수입니다")
    private String formId; // 구글 폼 ID (GoogleForm과 연결)

    @NotBlank(message = "지원자 이름은 필수입니다")
    private String applicantName;

    @NotBlank(message = "지원자 이메일은 필수입니다")
    @Email(message = "올바른 이메일 형식이 아닙니다")
    private String applicantEmail;

    @NotBlank(message = "구글 폼 응답 ID는 필수입니다")
    private String formResponseId;

    @NotNull(message = "제출 시간은 필수입니다")
    private LocalDateTime submissionTimestamp;

    // 추가된 개별 필드들
    private String school; // 학교
    private String department; // 학과
    private String grade; // 학년
    private String major; // 전공
    private String phoneNumber; // 전화번호

    // 유연한 폼 데이터 (JSON)
    @NotNull(message = "폼 데이터는 필수입니다")
    private Map<String, Object> formData;

    // DTO를 Entity로 변환
    // GoogleForm 엔티티는 Service에서 별도로 조회해서 설정해야 함
    public WebhookApplication toEntity(GoogleForm googleForm) {
        return WebhookApplication.builder()
                .googleForm(googleForm)
                .applicantName(this.applicantName)
                .applicantEmail(this.applicantEmail)
                .formResponseId(this.formResponseId)
                .submissionTimestamp(this.submissionTimestamp)
                .formData(this.formData)
                .school(this.school)
                .department(this.department)
                .grade(this.grade)
                .major(this.major)
                .phoneNumber(this.phoneNumber)
                .build();
    }

    // 폼 데이터에서 특정 값 조회
    public Object getFormValue(String key) {
        return formData != null ? formData.get(key) : null;
    }

    // 폼 데이터에서 문자열 값 조회
    public String getFormValueAsString(String key) {
        Object value = getFormValue(key);
        return value != null ? value.toString() : null;
    }
}

