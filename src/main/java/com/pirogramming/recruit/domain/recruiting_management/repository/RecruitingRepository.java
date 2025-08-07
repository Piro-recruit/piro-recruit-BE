package com.pirogramming.recruit.domain.recruiting_management.repository;

import com.pirogramming.recruit.domain.recruiting_management.entity.Recruiting;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecruitingRepository extends JpaRepository<Recruiting, Long> {

}
