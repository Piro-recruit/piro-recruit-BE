package com.pirogramming.recruit.domain.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class CreateGeneralAdminBatchResponse {
    private int totalCreated;                           // 생성된 총 개수
    private List<GeneralAdminResponse> createdAdmins;   // 생성된 Admin 목록
}