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

    @Column(nullable = false)
    private Integer generation; // 기수 (23, 24, 25기 등)

    @Builder
    public GoogleForm(String formId, String title, String formUrl, String sheetUrl, String description, Integer generation) {
        this.formId = formId;
        this.title = title;
        this.formUrl = formUrl;
        this.sheetUrl = sheetUrl;
        this.description = description;
        this.generation = generation;
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
        validateUrl(newUrl, "폼 URL");
        this.formUrl = newUrl;
    }

    // 시트 URL 업데이트
    public void updateSheetUrl(String newUrl) {
        if (newUrl != null) { // sheetUrl은 nullable이므로 null 체크
            validateUrl(newUrl, "시트 URL");
        }
        this.sheetUrl = newUrl;
    }

    // 기수 업데이트
    public void updateGeneration(Integer newGeneration) {
        if (newGeneration == null || newGeneration <= 0) {
            throw new IllegalArgumentException("기수는 1 이상의 양수여야 합니다");
        }
        this.generation = newGeneration;
    }

    // URL 유효성 검증
    private void validateUrl(String url, String fieldName) {
        if (url == null || url.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + "은 필수입니다");
        }
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            throw new IllegalArgumentException(fieldName + "은 올바른 URL 형식이어야 합니다");
        }
    }
}