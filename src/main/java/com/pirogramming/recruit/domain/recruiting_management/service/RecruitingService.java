package com.pirogramming.recruit.domain.recruiting_management.service;

import com.pirogramming.recruit.domain.recruiting_management.dto.RecruitingCreateRequest;
import com.pirogramming.recruit.domain.recruiting_management.entity.Recruiting;
import com.pirogramming.recruit.domain.recruiting_management.repository.RecruitingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RecruitingService {
    private final RecruitingRepository recruitingRepository;

    public List<Recruiting> getAllRecruitings() {
        return recruitingRepository.findAll();
    }

    public Recruiting createRecruiting(RecruitingCreateRequest request) {
        Recruiting recruiting = Recruiting.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .build();
        return recruitingRepository.save(recruiting);
    }
}
