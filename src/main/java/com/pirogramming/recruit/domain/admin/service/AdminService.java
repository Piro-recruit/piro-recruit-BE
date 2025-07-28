package com.pirogramming.recruit.domain.admin.service;

import com.pirogramming.recruit.domain.admin.dto.LoginRequest;
import com.pirogramming.recruit.domain.admin.dto.LoginResponse;
import com.pirogramming.recruit.domain.admin.entity.Admin;
import com.pirogramming.recruit.domain.admin.entity.AdminRole;
import com.pirogramming.recruit.domain.admin.repository.AdminRepository;
import com.pirogramming.recruit.global.exception.RecruitException;
import com.pirogramming.recruit.global.jwt.JwtTokenProvider;
import com.pirogramming.recruit.global.exception.code.ErrorCode;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminService {
    private final AdminRepository adminRepository;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public LoginResponse login(LoginRequest requestDto) {
        Admin admin = adminRepository.findByLoginCode(requestDto.getLoginCode())
                .orElseThrow(() -> new RecruitException(HttpStatus.UNAUTHORIZED, ErrorCode.INVALID_LOGIN_CODE));

        if (admin.getRole() == AdminRole.GENERAL && admin.isExpired()) {
            adminRepository.delete(admin);
            throw new RecruitException(HttpStatus.FORBIDDEN, ErrorCode.EXPIRED_ADMIN);
        }

        String token = jwtTokenProvider.generateToken(admin);
        return new LoginResponse("로그인 성공", token);
    }
}
