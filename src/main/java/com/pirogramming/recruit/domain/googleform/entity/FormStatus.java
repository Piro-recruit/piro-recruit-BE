package com.pirogramming.recruit.domain.googleform.entity;

import java.util.Set;
import java.util.EnumSet;

public enum FormStatus {
    ACTIVE("활성"),      // 활성 상태 (리쿠르팅 진행 중)
    INACTIVE("비활성"),  // 비활성 상태 (준비 중 또는 일시 정지)
    CLOSED("마감");      // 마감 상태 (리쿠르팅 종료)

    private final String description;

    FormStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 현재 상태에서 변경 가능한 상태들을 반환
     */
    public Set<FormStatus> getAllowedTransitions() {
        return switch (this) {
            case INACTIVE -> EnumSet.of(ACTIVE, CLOSED);
            case ACTIVE -> EnumSet.of(INACTIVE, CLOSED);
            case CLOSED -> EnumSet.of(INACTIVE, ACTIVE);
        };
    }

    /**
     * 특정 상태로 전환이 가능한지 확인
     */
    public boolean canTransitionTo(FormStatus target) {
        if (this == target) {
            return false; // 같은 상태로는 전환 불가
        }
        return getAllowedTransitions().contains(target);
    }

    /**
     * 상태 전환 검증
     */
    public void validateTransition(FormStatus target, String reason) {
        if (!canTransitionTo(target)) {
            throw new IllegalStateException(
                String.format("상태 전환이 불가능합니다: %s -> %s (사유: %s)", 
                    this.description, target.description, reason)
            );
        }
    }
}