package com.pirogramming.recruit.domain.admin.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pirogramming.recruit.domain.admin.entity.RefreshToken;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByAdminId(Long adminId);
    Optional<RefreshToken> findByToken(String token);
    void deleteByAdminId(Long adminId);
}