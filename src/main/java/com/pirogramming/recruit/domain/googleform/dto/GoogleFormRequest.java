package com.pirogramming.recruit.domain.googleform.dto;

import com.pirogramming.recruit.domain.googleform.entity.GoogleForm;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.URL;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class GoogleFormRequest {

    @NotBlank(message = "구글 폼 ID는 필수입니다")
    @Size(max = 100, message = "폼 ID는 100자 이하여야 합니다")
    private String formId;

    @NotBlank(message = "폼 제목은 필수입니다")
    @Size(min = 2, max = 100, message = "폼 제목은 2자 이상 100자 이하여야 합니다")
    private String title;

    @NotBlank(message = "구글 폼 URL은 필수입니다")
    @URL(message = "올바른 URL 형식이 아닙니다")
    @Size(max = 1000, message = "URL은 1000자 이하여야 합니다")
    private String formUrl;

    @URL(message = "올바른 URL 형식이 아닙니다")
    @Size(max = 1000, message = "URL은 1000자 이하여야 합니다")
    private String sheetUrl;

    @Size(max = 2000, message = "설명은 2000자 이하여야 합니다")
    private String description;

    @NotNull(message = "기수는 필수입니다")
    @Min(value = 1, message = "기수는 1 이상이어야 합니다")
    private Integer generation;

    // DTO를 Entity로 변환
    public GoogleForm toEntity() {
        return GoogleForm.builder()
                .formId(this.formId)
                .title(this.title)
                .formUrl(this.formUrl)
                .sheetUrl(this.sheetUrl)
                .description(this.description)
                .generation(this.generation)
                .build();
    }
}