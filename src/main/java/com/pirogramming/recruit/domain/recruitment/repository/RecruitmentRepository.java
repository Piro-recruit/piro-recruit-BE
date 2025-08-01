package com.pirogramming.recruit.domain.recruitment.repository;

import com.pirogramming.recruit.domain.recruitment.entity.Recruitment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface RecruitmentRepository extends JpaRepository<Recruitment, Long> {

    // 현재 활성화된 리크루팅 조회
    Optional<Recruitment> findByIsActiveTrue();

    // 상태별 리크루팅 조회
    List<Recruitment> findByStatus(Recruitment.RecruitmentStatus status);

    // 현재 지원 기간인 리크루팅 조회
    @Query("SELECT r FROM Recruitment r WHERE :currentDate BETWEEN r.startDate AND r.endDate")
    List<Recruitment> findActiveRecruitmentsByDate(LocalDate currentDate);

    // 최신 리크루팅 조회 (생성일 기준)
    @Query("SELECT r FROM Recruitment r ORDER BY r.createdAt DESC")
    List<Recruitment> findAllOrderByCreatedAtDesc();

    // 제목으로 리크루팅 검색
    List<Recruitment> findByTitleContaining(String title);
}