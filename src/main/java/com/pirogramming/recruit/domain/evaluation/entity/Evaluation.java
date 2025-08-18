package com.pirogramming.recruit.domain.evaluation.entity;

import com.pirogramming.recruit.domain.admin.entity.Admin;
import com.pirogramming.recruit.domain.webhook.entity.WebhookApplication;
import com.pirogramming.recruit.global.entity.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "evaluations",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"application_id", "evaluator_id"})
    })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Evaluation extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = false)
    private WebhookApplication application;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evaluator_id", nullable = false)
    private Admin evaluator;

    @Min(0)
    @Max(100)
    @Column(nullable = false)
    private Integer score;

    @Column(columnDefinition = "TEXT")
    private String comment;

    @Builder
    public Evaluation(WebhookApplication application, Admin evaluator, Integer score, String comment) {
        this.application = application;
        this.evaluator = evaluator;
        this.score = score;
        this.comment = comment;
    }

    public void updateEvaluation(Integer score, String comment) {
        this.score = score;
        this.comment = comment;
    }
}
