package com.pirogramming.recruit.domain.googleform.entity;

import com.pirogramming.recruit.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "google_forms")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GoogleForm extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String formId; // 구글 폼 고유 ID

    @Column(nullable = false)
    private String title; // "23기 리크루팅", "24기 리크루팅"

    @Column(nullable = false, length = 1000)
    private String formUrl; // 구글 폼 URL

    @Column(length = 1000)
    private String sheetUrl; // 연결된 구글 시트 URL

    @Column(nullable = false)
    private Boolean isActive = false; // 현재 사용 중인 폼인지

    @Column(columnDefinition = "TEXT")
    private String description; // 폼 설명

    @Builder
    public GoogleForm(String formId, String title, String formUrl, String sheetUrl, String description) {
        this.formId = formId;
        this.title = title;
        this.formUrl = formUrl;
        this.sheetUrl = sheetUrl;
        this.description = description;
        this.isActive = false;
    }

    // 폼 활성화 (기존 활성화된 것은 비활성화)
    public void activate() {
        this.isActive = true;
    }

    // 폼 비활성화
    public void deactivate() {
        this.isActive = false;
    }

    // 폼 URL 업데이트
    public void updateFormUrl(String newUrl) {
        this.formUrl = newUrl;
    }

    // 시트 URL 업데이트
    public void updateSheetUrl(String newUrl) {
        this.sheetUrl = newUrl;
    }
}