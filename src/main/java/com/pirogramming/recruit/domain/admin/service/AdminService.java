package com.pirogramming.recruit.domain.admin.service;

import com.pirogramming.recruit.domain.admin.dto.LoginRequest;
import com.pirogramming.recruit.domain.admin.dto.LoginResponse;
import com.pirogramming.recruit.domain.admin.entity.Admin;
import com.pirogramming.recruit.domain.admin.entity.AdminRole;
import com.pirogramming.recruit.domain.admin.entity.RefreshToken;
import com.pirogramming.recruit.domain.admin.repository.AdminRepository;
import com.pirogramming.recruit.domain.admin.repository.RefreshTokenRepository;
import com.pirogramming.recruit.global.exception.RecruitException;
import com.pirogramming.recruit.global.exception.code.ErrorCode;
import com.pirogramming.recruit.global.jwt.JwtTokenProvider;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminService {
    private final AdminRepository adminRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public LoginResponse login(LoginRequest requestDto) {
        Admin admin = adminRepository.findByLoginCode(requestDto.getLoginCode())
                .orElseThrow(() -> new RecruitException(HttpStatus.UNAUTHORIZED, ErrorCode.INVALID_LOGIN_CODE));

        if (admin.getRole() == AdminRole.GENERAL && admin.isExpired()) {
            adminRepository.delete(admin);
            throw new RecruitException(HttpStatus.FORBIDDEN, ErrorCode.EXPIRED_ADMIN);
        }

        // 토큰 생성
        String accessToken = jwtTokenProvider.generateAccessToken(admin.getId(), admin.getRole());
        String refreshToken = jwtTokenProvider.generateRefreshToken(admin.getId());

        // 기존 refreshToken 제거 후 새로 저장
        refreshTokenRepository.deleteByAdminId(admin.getId());
        refreshTokenRepository.save(new RefreshToken(admin.getId(), refreshToken));

        return new LoginResponse(accessToken, refreshToken);
    }

    public LoginResponse reissue(String refreshToken) {
        RefreshToken saved = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new RecruitException(HttpStatus.UNAUTHORIZED, ErrorCode.INVALID_REFRESH_TOKEN));

        Admin admin = adminRepository.findById(saved.getAdminId())
                .orElseThrow(() -> new RecruitException(HttpStatus.NOT_FOUND, ErrorCode.ADMIN_NOT_FOUND));

        String newAccessToken = jwtTokenProvider.generateAccessToken(admin.getId(), admin.getRole());
        return new LoginResponse(newAccessToken, refreshToken); // refreshToken은 그대로
    }
}
