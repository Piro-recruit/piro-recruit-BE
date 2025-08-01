package com.pirogramming.recruit.domain.recruitment.entity;

import com.pirogramming.recruit.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "recruitments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Recruitment extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title; // 예: "피로그래밍 17기", "피로그래밍 18기"

    @Column(columnDefinition = "TEXT")
    private String description; // 리크루팅 설명

    @Column(nullable = false)
    private LocalDate startDate; // 지원 시작일

    @Column(nullable = false)
    private LocalDate endDate; // 지원 마감일

    @Column(nullable = false, length = 1000)
    private String googleFormUrl; // 구글 폼 URL

    @Column(length = 1000)
    private String googleSheetUrl; // 구글 시트 URL

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RecruitmentStatus status = RecruitmentStatus.DRAFT;

    @Column(nullable = false)
    private Boolean isActive = false; // 현재 활성화된 리크루팅인지

    @Builder
    public Recruitment(String title, String description, LocalDate startDate,
                       LocalDate endDate, String googleFormUrl, String googleSheetUrl) {
        this.title = title;
        this.description = description;
        this.startDate = startDate;
        this.endDate = endDate;
        this.googleFormUrl = googleFormUrl;
        this.googleSheetUrl = googleSheetUrl;
        this.status = RecruitmentStatus.DRAFT;
        this.isActive = false;
    }

    // 리크루팅 활성화 (현재 진행 중인 리크루팅으로 설정)
    public void activate() {
        this.isActive = true;
        this.status = RecruitmentStatus.ACTIVE;
    }

    // 리크루팅 비활성화
    public void deactivate() {
        this.isActive = false;
    }

    // 리크루팅 완료 처리
    public void complete() {
        this.status = RecruitmentStatus.COMPLETED;
        this.isActive = false;
    }

    // 현재 지원 기간인지 확인
    public boolean isApplicationPeriod() {
        LocalDate now = LocalDate.now();
        return !now.isBefore(startDate) && !now.isAfter(endDate);
    }

    // Google Form URL 업데이트
    public void updateGoogleFormUrl(String newUrl) {
        this.googleFormUrl = newUrl;
    }

    public enum RecruitmentStatus {
        DRAFT,      // 초안 (아직 공개 안됨)
        ACTIVE,     // 활성화 (지원 가능)
        COMPLETED,  // 완료됨
        CANCELLED   // 취소됨
    }
}