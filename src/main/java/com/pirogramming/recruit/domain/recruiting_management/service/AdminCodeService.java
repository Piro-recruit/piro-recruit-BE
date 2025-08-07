package com.pirogramming.recruit.domain.recruiting_management.service;

import com.pirogramming.recruit.domain.recruiting_management.entity.AdminCode;
import com.pirogramming.recruit.domain.recruiting_management.entity.Recruiting;
import com.pirogramming.recruit.domain.recruiting_management.repository.AdminCodeRepository;
import com.pirogramming.recruit.domain.recruiting_management.repository.RecruitingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminCodeService {
    private final AdminCodeRepository adminCodeRepository;
    private final RecruitingRepository recruitingRepository;

    public void createAdminCodes(Long recruitingId, int count) {
        Recruiting recruiting = recruitingRepository.findById(recruitingId)
                .orElseThrow(() -> new IllegalArgumentException("리쿠르팅이 존재하지 않습니다."));

        List<AdminCode> codes = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            AdminCode code = AdminCode.builder()
                    .code(UUID.randomUUID().toString())
                    .recruiting(recruiting)
                    .expiredAt(recruiting.getEndDate().atTime(23, 59))
                    .build();
            codes.add(code);
        }
        adminCodeRepository.saveAll(codes);
    }
}
