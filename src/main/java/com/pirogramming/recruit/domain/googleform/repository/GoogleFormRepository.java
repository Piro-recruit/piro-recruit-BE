package com.pirogramming.recruit.domain.googleform.repository;

import com.pirogramming.recruit.domain.googleform.entity.GoogleForm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    // 제목으로 구글 폼 검색 (대소문자 구분)
    List<GoogleForm> findByTitleContaining(String title);

    // 제목으로 구글 폼 검색 (대소문자 무시)
    List<GoogleForm> findByTitleContainingIgnoreCase(String title);

    // 폼 ID 존재 여부 확인
    boolean existsByFormId(String formId);

    // 모든 구글 폼을 비활성화 (원자적 연산)
    @Modifying
    @Query("UPDATE GoogleForm g SET g.isActive = false WHERE g.isActive = true")
    int deactivateAllGoogleForms();

    // 특정 기수의 구글 폼 조회
    List<GoogleForm> findByGenerationOrderByCreatedAtDesc(Integer generation);

    // 특정 기수의 활성화된 구글 폼 조회
    Optional<GoogleForm> findByGenerationAndIsActiveTrue(Integer generation);

    // 특정 기수에 구글 폼이 존재하는지 확인
    boolean existsByGeneration(Integer generation);

    // 기수별 구글 폼 개수 조회
    long countByGeneration(Integer generation);

    // 현재 활성화된 구글 폼의 기수 조회
    @Query("SELECT g.generation FROM GoogleForm g WHERE g.isActive = true")
    Optional<Integer> findCurrentActiveGeneration();

    // 가장 최신 기수 조회
    @Query("SELECT MAX(g.generation) FROM GoogleForm g")
    Optional<Integer> findMaxGeneration();

    // 기수 범위로 구글 폼 조회
    @Query("SELECT g FROM GoogleForm g WHERE g.generation BETWEEN :startGen AND :endGen ORDER BY g.generation DESC, g.createdAt DESC")
    List<GoogleForm> findByGenerationBetweenOrderByGenerationDescCreatedAtDesc(
            @Param("startGen") Integer startGeneration,
            @Param("endGen") Integer endGeneration);
}