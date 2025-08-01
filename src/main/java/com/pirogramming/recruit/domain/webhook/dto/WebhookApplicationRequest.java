package com.pirogramming.recruit.domain.webhook.dto;

import com.pirogramming.recruit.domain.webhook.entity.WebhookApplication;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Map;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class WebhookApplicationRequest {

    @NotNull(message = "리크루팅 기수는 필수입니다")
    private Long recruitmentId;

    @NotBlank(message = "지원자 이름은 필수입니다")
    private String applicantName;

    @NotBlank(message = "지원자 이메일은 필수입니다")
    @Email(message = "올바른 이메일 형식이 아닙니다")
    private String applicantEmail;

    @NotBlank(message = "구글 폼 응답 ID는 필수입니다")
    private String formResponseId;

    @NotBlank(message = "제출 시간은 필수입니다")
    private String submissionTimestamp;

    // 유연한 폼 데이터 (JSON)
    @NotNull(message = "폼 데이터는 필수입니다")
    private Map<String, Object> formData;

    // DTO를 Entity로 변환
    // Recruitment 엔티티는 Service에서 별도로 조회해서 설정해야 함
    public WebhookApplication toEntity(com.pirogramming.recruit.domain.recruitment.entity.Recruitment recruitment) {
        return WebhookApplication.builder()
                .recruitment(recruitment)
                .applicantName(this.applicantName)
                .applicantEmail(this.applicantEmail)
                .formResponseId(this.formResponseId)
                .submissionTimestamp(this.submissionTimestamp)
                .formData(this.formData)
                .build();
    }

    // 폼 데이터에서 특정 값 조회(원본의 타입을 그대로 반환 -> 타입별로 처리가 필요할 때)
    public Object getFormValue(String key) {
        return formData != null ? formData.get(key) : null;
    }

    // 폼 데이터에서 문자열 값 조회(모두 문자열로 변환)
    public String getFormValueAsString(String key) {
        Object value = getFormValue(key);
        return value != null ? value.toString() : null;
    }
}