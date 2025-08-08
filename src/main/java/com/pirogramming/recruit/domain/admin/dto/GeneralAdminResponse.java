package com.pirogramming.recruit.domain.admin.dto;

import com.pirogramming.recruit.domain.admin.entity.Admin;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class GeneralAdminResponse {
    private Long id;
    private String loginCode;
    private String identifierName;
    private LocalDateTime expiredAt;
    private LocalDateTime createdAt;

    public GeneralAdminResponse(Admin admin) {
        this.id = admin.getId();
        this.loginCode = admin.getLoginCode();
        this.identifierName = admin.getIdentifierName();
        this.expiredAt = admin.getExpiredAt();
        this.createdAt = admin.getCreatedAt();
    }
}