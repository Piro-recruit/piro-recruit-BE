package com.pirogramming.recruit.domain.googleform.repository;

import com.pirogramming.recruit.domain.googleform.entity.GoogleForm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GoogleFormRepository extends JpaRepository<GoogleForm, Long> {

    // 폼 ID로 조회
    Optional<GoogleForm> findByFormId(String formId);

    // 현재 활성화된 구글 폼 조회
    Optional<GoogleForm> findByIsActiveTrue();

    // 활성화된 모든 구글 폼 조회
    List<GoogleForm> findByIsActiveTrueOrderByCreatedAtDesc();

    // 최신 구글 폼 조회 (생성일 기준)
    @Query("SELECT g FROM GoogleForm g ORDER BY g.createdAt DESC")
    List<GoogleForm> findAllOrderByCreatedAtDesc();

    // 제목으로 구글 폼 검색
    List<GoogleForm> findByTitleContaining(String title);

    // 폼 ID 존재 여부 확인
    boolean existsByFormId(String formId);
}