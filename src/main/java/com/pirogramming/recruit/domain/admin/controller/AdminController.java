package com.pirogramming.recruit.domain.admin.controller;

import com.pirogramming.recruit.domain.admin.dto.LoginRequest;
import com.pirogramming.recruit.domain.admin.dto.LoginResponse;
import com.pirogramming.recruit.domain.admin.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<LoginResponse> refresh(@RequestBody String refreshToken) {
        return ResponseEntity.ok(adminService.reissue(refreshToken));
    }
}
