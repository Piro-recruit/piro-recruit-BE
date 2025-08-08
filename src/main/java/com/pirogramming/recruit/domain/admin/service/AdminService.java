package com.pirogramming.recruit.domain.admin.service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.pirogramming.recruit.domain.admin.dto.CreateGeneralAdminBatchRequest;
import com.pirogramming.recruit.domain.admin.dto.CreateGeneralAdminBatchResponse;
import com.pirogramming.recruit.domain.admin.dto.CreateGeneralAdminRequest;
import com.pirogramming.recruit.domain.admin.dto.GeneralAdminResponse;
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
            // RefreshToken 먼저 삭제
            refreshTokenRepository.deleteByAdminId(admin.getId());
            // Admin 삭제
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

    @Transactional
    public LoginResponse reissue(String refreshToken) {
        RefreshToken saved = refreshTokenRepository.findByToken(refreshToken)
            .orElseThrow(() -> new RecruitException(HttpStatus.UNAUTHORIZED, ErrorCode.INVALID_REFRESH_TOKEN));

        // RefreshToken 만료 검증
        if (saved.isExpired()) {
            refreshTokenRepository.delete(saved);
            throw new RecruitException(HttpStatus.UNAUTHORIZED, ErrorCode.INVALID_REFRESH_TOKEN);
        }

        Admin admin = adminRepository.findById(saved.getAdminId())
            .orElseThrow(() -> new RecruitException(HttpStatus.NOT_FOUND, ErrorCode.ADMIN_NOT_FOUND));

        // Token Rotation: 새로운 Access Token과 Refresh Token 모두 발급
        String newAccessToken = jwtTokenProvider.generateAccessToken(admin.getId(), admin.getRole());
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(admin.getId());

        // 기존 RefreshToken 삭제하고 새로운 RefreshToken 저장
        refreshTokenRepository.deleteByAdminId(admin.getId());
        refreshTokenRepository.save(new RefreshToken(admin.getId(), newRefreshToken));

        return new LoginResponse(newAccessToken, newRefreshToken);
    }

    @Transactional
    public GeneralAdminResponse createGeneralAdmin(CreateGeneralAdminRequest request) {
        // 고유한 로그인 코드 생성 (UUID 기반)
        String loginCode = UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        // 중복 체크 (매우 낮은 확률이지만)
        while (adminRepository.findByLoginCode(loginCode).isPresent()) {
            loginCode = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        }

        // identifierName이 제공되지 않으면 자동 생성
        String identifierName = request.getIdentifierName();
        if (identifierName == null || identifierName.trim().isEmpty()) {
            int nextNumber = adminRepository.findMaxIdentifierNumber().orElse(0) + 1;
            identifierName = String.format("평가자-%03d", nextNumber);
        }

        Admin admin = Admin.builder()
            .loginCode(loginCode)
            .identifierName(identifierName)
            .role(AdminRole.GENERAL)
            .expiredAt(request.getExpiredAt())
            .build();

        Admin saved = adminRepository.save(admin);
        return new GeneralAdminResponse(saved);
    }

    @Transactional
    public CreateGeneralAdminBatchResponse createGeneralAdminBatch(CreateGeneralAdminBatchRequest request) {
        List<GeneralAdminResponse> createdAdmins = new ArrayList<>();

        // 현재 존재하는 가장 큰 식별자 번호 찾기
        int startNumber = adminRepository.findMaxIdentifierNumber().orElse(0) + 1;

        for (int i = 0; i < request.getCount(); i++) {
            // 고유한 로그인 코드 생성 (UUID 기반)
            String loginCode = UUID.randomUUID().toString().substring(0, 8).toUpperCase();

            // 중복 체크 (매우 낮은 확률이지만)
            while (adminRepository.findByLoginCode(loginCode).isPresent()) {
                loginCode = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            }

            // 자동 식별자 생성 (기존 최대값의 다음 번호부터)
            String identifierName = String.format("평가자-%03d", startNumber + i);

            Admin admin = Admin.builder()
                .loginCode(loginCode)
                .identifierName(identifierName)
                .role(AdminRole.GENERAL)
                .expiredAt(request.getExpiredAt())
                .build();

            Admin saved = adminRepository.save(admin);
            createdAdmins.add(new GeneralAdminResponse(saved));
        }

        return new CreateGeneralAdminBatchResponse(request.getCount(), createdAdmins);
    }

    @Transactional
    public List<GeneralAdminResponse> getAllGeneralAdmins() {
        return adminRepository.findAll().stream()
            .filter(admin -> admin.getRole() == AdminRole.GENERAL)
            .map(GeneralAdminResponse::new)
            .collect(Collectors.toList());
    }

    @Transactional
    public void deleteExpiredGeneralAdmins() {
        List<Admin> expiredAdmins = adminRepository.findAll().stream()
            .filter(Admin::isExpired)
            .collect(Collectors.toList());

        // 관련된 RefreshToken들 먼저 삭제
        for (Admin admin : expiredAdmins) {
            refreshTokenRepository.deleteByAdminId(admin.getId());
        }

        // 만료된 Admin 삭제
        adminRepository.deleteAll(expiredAdmins);
    }

    @Transactional
    public void deleteAllGeneralAdmins() {
        List<Admin> generalAdmins = adminRepository.findAll().stream()
            .filter(admin -> admin.getRole() == AdminRole.GENERAL)
            .collect(Collectors.toList());

        // 관련된 RefreshToken들 먼저 삭제
        for (Admin admin : generalAdmins) {
            refreshTokenRepository.deleteByAdminId(admin.getId());
        }

        // 모든 General Admin 삭제
        adminRepository.deleteAll(generalAdmins);
    }
}