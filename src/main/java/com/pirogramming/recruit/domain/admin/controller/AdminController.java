package com.pirogramming.recruit.domain.admin.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pirogramming.recruit.domain.admin.dto.CreateGeneralAdminRequest;
import com.pirogramming.recruit.domain.admin.dto.GeneralAdminResponse;
import com.pirogramming.recruit.domain.admin.dto.LoginRequest;
import com.pirogramming.recruit.domain.admin.dto.LoginResponse;
import com.pirogramming.recruit.domain.admin.dto.RefreshTokenRequest;
import com.pirogramming.recruit.domain.admin.service.AdminService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {
    private final AdminService adminService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(adminService.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(@RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(adminService.reissue(request.getRefreshToken()));
    }

    // General Admin 생성 (ROOT 권한 필요)
    @PostMapping("/general")
    @PreAuthorize("hasRole('ROOT')")
    public ResponseEntity<GeneralAdminResponse> createGeneralAdmin(@RequestBody CreateGeneralAdminRequest request) {
        return ResponseEntity.ok(adminService.createGeneralAdmin(request));
    }

    // 모든 General Admin 조회 (ROOT 권한 필요)
    @GetMapping("/general")
    @PreAuthorize("hasRole('ROOT')")
    public ResponseEntity<List<GeneralAdminResponse>> getAllGeneralAdmins() {
        return ResponseEntity.ok(adminService.getAllGeneralAdmins());
    }

    // 만료된 General Admin 삭제 (ROOT 권한 필요)
    @DeleteMapping("/general/expired")
    @PreAuthorize("hasRole('ROOT')")
    public ResponseEntity<Void> deleteExpiredGeneralAdmins() {
        adminService.deleteExpiredGeneralAdmins();
        return ResponseEntity.ok().build();
    }

    // 모든 General Admin 일괄 삭제 (ROOT 권한 필요)
    @DeleteMapping("/general/all")
    @PreAuthorize("hasRole('ROOT')")
    public ResponseEntity<Void> deleteAllGeneralAdmins() {
        adminService.deleteAllGeneralAdmins();
        return ResponseEntity.ok().build();
    }
}
