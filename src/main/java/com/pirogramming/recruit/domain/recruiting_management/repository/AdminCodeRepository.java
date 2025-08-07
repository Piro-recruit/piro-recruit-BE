package com.pirogramming.recruit.domain.recruiting_management.repository;

import com.pirogramming.recruit.domain.recruiting_management.entity.AdminCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AdminCodeRepository extends JpaRepository<AdminCode, Long> {
    List<AdminCode> findByRecruitingId(Long recruitingId);
}
