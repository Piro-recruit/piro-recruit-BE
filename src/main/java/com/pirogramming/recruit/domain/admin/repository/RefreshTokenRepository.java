package com.pirogramming.recruit.domain.admin.repository;

import com.pirogramming.recruit.domain.admin.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByAdminId(Long adminId);
    Optional<RefreshToken> findByToken(String token);
    void deleteByAdminId(Long adminId);
}
