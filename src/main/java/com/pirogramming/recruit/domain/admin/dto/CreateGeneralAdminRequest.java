package com.pirogramming.recruit.domain.admin.dto;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class CreateGeneralAdminRequest {
    private String identifierName;
    private LocalDateTime expiredAt;
}