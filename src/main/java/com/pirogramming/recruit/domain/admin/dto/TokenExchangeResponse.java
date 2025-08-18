package com.pirogramming.recruit.domain.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TokenExchangeResponse {
    
    private String accessToken;
    private String tokenType; // "Bearer"
    private Long expiresIn; // 만료 시간 (초 단위)
    private String purpose; // 토큰 용도
}