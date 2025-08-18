package com.pirogramming.recruit.domain.ai_summary.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.pirogramming.recruit.domain.webhook.entity.WebhookApplication;
import com.pirogramming.recruit.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.LinkedHashMap;
import java.util.Map;

@Entity
@Table(name = "application_summary")
@Getter
@NoArgsConstructor
public class ApplicationSummary extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // WebhookApplication과 1:1 관계
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "webhook_application_id", nullable = false, unique = true)
    @JsonIgnore  // JSON 직렬화 시 순환참조 방지
    private WebhookApplication webhookApplication;

    // 요약 결과를 Key-Value로 저장
    @ElementCollection
    @CollectionTable(name = "application_summary_items", joinColumns = @JoinColumn(name = "summary_id"))
    @MapKeyColumn(name = "item_key", length = 100)
    @Column(name = "item_value", columnDefinition = "TEXT")
    private Map<String, String> items = new LinkedHashMap<>();

    @Builder
    public ApplicationSummary(WebhookApplication webhookApplication,
                              Map<String, String> items) {
        this.webhookApplication = webhookApplication;
        if (items != null) this.items.putAll(items);
    }
}
