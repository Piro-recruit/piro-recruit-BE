package com.pirogramming.recruit.domain.googleform.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.pirogramming.recruit.global.entity.BaseTimeEntity;
import com.pirogramming.recruit.domain.webhook.entity.WebhookApplication;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FormStatus status = FormStatus.INACTIVE; // 폼 상태 (활성/비활성/마감)

    @Column(columnDefinition = "TEXT")
    private String description; // 폼 설명

    @Column
    private LocalDateTime recruitingStartDate; // 리쿠르팅 시작 날짜

    @Column
    private LocalDateTime recruitingEndDate; // 리쿠르팅 종료 날짜

    @Column(nullable = false)
    private Integer generation; // 기수 (23, 24, 25기 등)

    // 연관된 지원서들 (구글 폼 삭제 시 함께 삭제)
    @OneToMany(mappedBy = "googleForm", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore  // JSON 직렬화 시 순환참조 방지
    private List<WebhookApplication> applications = new ArrayList<>();

    @Builder
    public GoogleForm(String formId, String title, String formUrl, String sheetUrl, String description, Integer generation, LocalDateTime recruitingStartDate, LocalDateTime recruitingEndDate) {
        this.formId = formId;
        this.title = title;
        this.formUrl = formUrl;
        this.sheetUrl = sheetUrl;
        this.description = description;
        this.recruitingStartDate = recruitingStartDate;
        this.recruitingEndDate = recruitingEndDate;
        this.generation = generation;
        this.status = FormStatus.INACTIVE;
    }

    // 폼 활성화 (기존 활성화된 것은 비활성화)
    public void activate() {
        changeStatus(FormStatus.ACTIVE, "폼 활성화");
    }

    // 폼 비활성화
    public void deactivate() {
        changeStatus(FormStatus.INACTIVE, "폼 비활성화");
    }

    // 폼 마감 (직접 API를 통해서만 가능)
    public void close() {
        changeStatus(FormStatus.CLOSED, "폼 마감");
    }

    // 상태 변경 (검증 포함)
    private void changeStatus(FormStatus newStatus, String reason) {
        if (this.status != null) {
            this.status.validateTransition(newStatus, reason);
        }
        this.status = newStatus;
    }

    // 수동 상태 변경 (관리자용)
    public void changeStatusManually(FormStatus newStatus, String reason) {
        changeStatus(newStatus, "관리자 수동 변경: " + reason);
    }

    // 삭제 가능 여부 확인
    public boolean canBeDeleted() {
        return this.status == FormStatus.INACTIVE;
    }

    // 활성화 가능 여부 검증
    public void validateCanActivate() {
        if (this.formUrl == null || this.formUrl.trim().isEmpty()) {
            throw new IllegalStateException("폼 URL이 설정되지 않은 구글 폼은 활성화할 수 없습니다");
        }
        
        if (this.generation == null || this.generation <= 0) {
            throw new IllegalStateException("유효한 기수가 설정되지 않은 구글 폼은 활성화할 수 없습니다");
        }
        
        if (this.title == null || this.title.trim().isEmpty()) {
            throw new IllegalStateException("제목이 설정되지 않은 구글 폼은 활성화할 수 없습니다");
        }
    }

    // 리쿠르팅 날짜 업데이트 (검증 포함)
    public void updateRecruitingDates(LocalDateTime start, LocalDateTime end) {
        if (start != null && end != null && start.isAfter(end)) {
            throw new IllegalArgumentException("리쿠르팅 시작일은 종료일보다 빨라야 합니다");
        }
        
        if (start != null && start.isBefore(LocalDateTime.now().minusDays(365))) {
            throw new IllegalArgumentException("리쿠르팅 시작일은 1년 이전으로 설정할 수 없습니다");
        }
        
        this.recruitingStartDate = start;
        this.recruitingEndDate = end;
    }

    // 현재 상태가 활성인지 확인
    public boolean isActive() {
        return this.status == FormStatus.ACTIVE;
    }

    // 현재 상태가 마감인지 확인  
    public boolean isClosed() {
        return this.status == FormStatus.CLOSED;
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