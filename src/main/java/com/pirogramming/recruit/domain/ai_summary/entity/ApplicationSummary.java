package com.pirogramming.recruit.domain.ai_summary.entity;

import com.pirogramming.recruit.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.LinkedHashMap;
import java.util.Map;

@Entity
@Table(
        name = "application_summary",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_formResponseId_email",
                columnNames = {"form_response_id", "applicant_email"}
        )
)
@Getter
@NoArgsConstructor
public class ApplicationSummary extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Google Form 식별자(조회 편의용)
    @Column(name = "form_id", length = 128, nullable = false)
    private String formId;

    // 각 제출 건의 응답 ID (지원서 고유 키 역할)
    @Column(name = "form_response_id", length = 128, nullable = false)
    private String formResponseId;

    @Column(name = "applicant_name", length = 100)
    private String applicantName;

    @Column(name = "applicant_email", length = 255, nullable = false)
    private String applicantEmail;

    // 요약 결과를 Key-Value로 저장
    @ElementCollection
    @CollectionTable(name = "application_summary_items", joinColumns = @JoinColumn(name = "summary_id"))
    @MapKeyColumn(name = "item_key", length = 100)
    @Column(name = "item_value", columnDefinition = "TEXT")
    private Map<String, String> items = new LinkedHashMap<>();

    @Builder
    public ApplicationSummary(String formId,
                              String formResponseId,
                              String applicantName,
                              String applicantEmail,
                              Map<String, String> items) {
        this.formId = formId;
        this.formResponseId = formResponseId;
        this.applicantName = applicantName;
        this.applicantEmail = applicantEmail;
        if (items != null) this.items.putAll(items);
    }
}
