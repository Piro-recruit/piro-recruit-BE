package com.pirogramming.recruit.domain.admin.entity;

import java.time.LocalDateTime;

import com.pirogramming.recruit.global.entity.BaseTimeEntity;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Admin extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String loginCode;

    private String identifierName;  // 평가를 위한 식별 이름 또는 번호

    @Enumerated(EnumType.STRING)
    private AdminRole role;  // enum 사용

    private LocalDateTime expiredAt;

    public boolean isExpired() {
        return role == AdminRole.GENERAL && expiredAt != null && expiredAt.isBefore(LocalDateTime.now());
    }

    public boolean isRoot() {
        return role == AdminRole.ROOT;
    }
}
