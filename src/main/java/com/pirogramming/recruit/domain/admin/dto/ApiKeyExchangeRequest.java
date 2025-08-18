package com.pirogramming.recruit.domain.admin.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ApiKeyExchangeRequest {
    
    @NotBlank(message = "API Key는 필수입니다.")
    private String apiKey;
    
    private String purpose; // 선택적: 용도 명시 (webhook, external-service 등)
}