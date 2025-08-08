package com.pirogramming.recruit.domain.admin.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Admin {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String loginCode;

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
