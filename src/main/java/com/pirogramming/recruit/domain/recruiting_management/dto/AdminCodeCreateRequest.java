package com.pirogramming.recruit.domain.recruiting_management.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AdminCodeCreateRequest {
    private int count; // 생성할 관리자 코드 수
}
