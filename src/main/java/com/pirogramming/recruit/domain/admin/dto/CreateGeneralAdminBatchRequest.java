package com.pirogramming.recruit.domain.admin.dto;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class CreateGeneralAdminBatchRequest {
    private int count;                    // 생성할 General Admin 수
    private LocalDateTime expiredAt;      // 만료 날짜
}