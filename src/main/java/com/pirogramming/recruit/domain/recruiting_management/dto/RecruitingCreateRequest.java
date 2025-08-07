package com.pirogramming.recruit.domain.recruiting_management.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RecruitingCreateRequest {
    private String title;
    private String description;
    private LocalDate startDate;
    private LocalDate endDate;
}
