package com.pirogramming.recruit.domain.admin.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long adminId;

    private String token;

    public RefreshToken(Long adminId, String token) {
        this.adminId = adminId;
        this.token = token;
    }

    public void updateToken(String newToken) {
        this.token = newToken;
    }
}
