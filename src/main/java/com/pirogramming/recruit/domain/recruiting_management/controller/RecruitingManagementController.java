package com.pirogramming.recruit.domain.recruiting_management.controller;

import com.pirogramming.recruit.domain.recruiting_management.dto.AdminCodeCreateRequest;
import com.pirogramming.recruit.domain.recruiting_management.dto.RecruitingCreateRequest;
import com.pirogramming.recruit.domain.recruiting_management.entity.Recruiting;
import com.pirogramming.recruit.domain.recruiting_management.service.AdminCodeService;
import com.pirogramming.recruit.domain.recruiting_management.service.RecruitingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/recruitings")
@RequiredArgsConstructor
public class RecruitingManagementController {
    private final RecruitingService recruitingService;
    private final AdminCodeService adminCodeService;

    @GetMapping
    public List<Recruiting> getAllRecruitings() {
        return recruitingService.getAllRecruitings();
    }

    @PostMapping
    public ResponseEntity<Recruiting> createRecruiting(@RequestBody RecruitingCreateRequest request) {
        return ResponseEntity.ok(recruitingService.createRecruiting(request));
    }

    @PostMapping("/{id}/admin-codes")
    @PreAuthorize("hasRole('ROOT')")
    public ResponseEntity<String> createAdminCodes(
            @PathVariable Long id,
            @RequestBody AdminCodeCreateRequest request) {
        adminCodeService.createAdminCodes(id, request.getCount());
        return ResponseEntity.ok("관리자 코드 생성 완료");
    }
}
