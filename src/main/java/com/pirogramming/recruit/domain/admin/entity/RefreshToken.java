package com.pirogramming.recruit.domain.admin.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long adminId;

    private String token;
    
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    public RefreshToken(Long adminId, String token) {
        this.adminId = adminId;
        this.token = token;
        this.createdAt = LocalDateTime.now();
    }

    public void updateToken(String newToken) {
        this.token = newToken;
        this.createdAt = LocalDateTime.now();
    }
    
    public boolean isExpired() {
        // 7일 후 만료
        return createdAt.plusDays(7).isBefore(LocalDateTime.now());
    }
}
