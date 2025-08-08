package com.pirogramming.recruit.domain.admin.repository;

import com.pirogramming.recruit.domain.admin.entity.Admin;
import com.pirogramming.recruit.domain.admin.entity.AdminRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface AdminRepository extends JpaRepository<Admin, Long> {
    Optional<Admin> findByLoginCode(String loginCode);
    List<Admin> findByRole(AdminRole role);
    
    @Query("SELECT MAX(CAST(SUBSTRING(a.identifierName, 5) AS int)) FROM Admin a WHERE a.role = 'GENERAL' AND a.identifierName LIKE '평가자-%'")
    Optional<Integer> findMaxIdentifierNumber();
}
