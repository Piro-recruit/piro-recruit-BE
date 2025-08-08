package com.pirogramming.recruit.domain.admin.repository;

import com.pirogramming.recruit.domain.admin.entity.Admin;
import com.pirogramming.recruit.domain.admin.entity.AdminRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AdminRepository extends JpaRepository<Admin, Long> {
    Optional<Admin> findByLoginCode(String loginCode);
    List<Admin> findByRole(AdminRole role);
}
