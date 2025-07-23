package com.pirogramming.recruit.domain.webhook.entity;

import com.pirogramming.recruit.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "webhook_applications")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WebhookApplication extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String phone;

    @Column(nullable = false)
    private String school;

    @Column(nullable = false)
    private String major;

    @Column(length = 1000)
    private String portfolioUrl;

    @Column(columnDefinition = "TEXT")
    private String introduction;

    @Column(columnDefinition = "TEXT")
    private String motivation;

    @Column(nullable = false)
    private String formResponseId; // 구글 폼 응답 ID

    @Column(nullable = false)
    private String submissionTimestamp; // 구글 폼 제출 시간

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProcessingStatus status = ProcessingStatus.PENDING;

    @Column(columnDefinition = "TEXT")
    private String errorMessage; // 처리 실패 시 오류 메시지

    @Builder
    public WebhookApplication(String name, String email, String phone, String school,
                              String major, String portfolioUrl, String introduction,
                              String motivation, String formResponseId, String submissionTimestamp) {
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.school = school;
        this.major = major;
        this.portfolioUrl = portfolioUrl;
        this.introduction = introduction;
        this.motivation = motivation;
        this.formResponseId = formResponseId;
        this.submissionTimestamp = submissionTimestamp;
        this.status = ProcessingStatus.PENDING;
    }

    public void markAsProcessed() {
        this.status = ProcessingStatus.COMPLETED;
    }

    public void markAsFailed(String errorMessage) {
        this.status = ProcessingStatus.FAILED;
        this.errorMessage = errorMessage;
    }

    public enum ProcessingStatus {
        PENDING,    // 처리 대기
        COMPLETED,  // 처리 완료
        FAILED      // 처리 실패
    }
}