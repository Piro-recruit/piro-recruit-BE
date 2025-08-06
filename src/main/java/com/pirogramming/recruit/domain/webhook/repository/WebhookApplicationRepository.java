package com.pirogramming.recruit.domain.webhook.repository;

import com.pirogramming.recruit.domain.webhook.entity.WebhookApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WebhookApplicationRepository extends JpaRepository<WebhookApplication, Long> {

    // 이메일로 지원서 조회
    Optional<WebhookApplication> findByApplicantEmail(String applicantEmail);

    // 구글 폼 응답 ID로 중복 체크
    boolean existsByFormResponseId(String formResponseId);

    // 이메일 존재 여부 확인
    boolean existsByApplicantEmail(String applicantEmail);

    // 구글 폼별 지원서 조회
    List<WebhookApplication> findByGoogleFormId(Long googleFormId);

    // 구글 폼별 + 상태별 지원서 조회
    List<WebhookApplication> findByGoogleFormIdAndStatus(Long googleFormId, WebhookApplication.ProcessingStatus status);

    // 처리 상태별 조회
    List<WebhookApplication> findByStatus(WebhookApplication.ProcessingStatus status);

    // 특정 상태의 지원서 개수 조회
    @Query("SELECT COUNT(w) FROM WebhookApplication w WHERE w.status = :status")
    long countByStatus(@Param("status") WebhookApplication.ProcessingStatus status);

    // 구글 폼별 지원서 개수 조회
    long countByGoogleFormId(Long googleFormId);

    // 처리 대기 중인 지원서 개수
    default long countPendingApplications() {
        return countByStatus(WebhookApplication.ProcessingStatus.PENDING);
    }

    // 최근 제출된 지원서 조회 (생성시간 기준 내림차순)
    @Query("SELECT w FROM WebhookApplication w ORDER BY w.createdAt DESC")
    List<WebhookApplication> findAllOrderByCreatedAtDesc();

    // 구글 폼별 최근 지원서 조회
    @Query("SELECT w FROM WebhookApplication w WHERE w.googleForm.id = :googleFormId ORDER BY w.createdAt DESC")
    List<WebhookApplication> findByGoogleFormIdOrderByCreatedAtDesc(@Param("googleFormId") Long googleFormId);

    // 폼 ID로 지원서 조회
    @Query("SELECT w FROM WebhookApplication w WHERE w.googleForm.formId = :formId ORDER BY w.createdAt DESC")
    List<WebhookApplication> findByFormIdOrderByCreatedAtDesc(@Param("formId") String formId);

    // 이름과 이메일로 지원서 존재 확인 (구글 폼별)
    boolean existsByGoogleFormIdAndApplicantNameAndApplicantEmail(Long googleFormId, String applicantName, String applicantEmail);

    // 구글 폼 ID로 지원서 존재 확인 (이메일 기준)
    boolean existsByGoogleFormIdAndApplicantEmail(Long googleFormId, String applicantEmail);

    // 폼 ID로 지원서 존재 확인 (이메일 기준)
    @Query("SELECT COUNT(w) > 0 FROM WebhookApplication w WHERE w.googleForm.formId = :formId AND w.applicantEmail = :email")
    boolean existsByFormIdAndApplicantEmail(@Param("formId") String formId, @Param("email") String email);

    // 구글 폼별 + 상태별 지원서 개수 조회 (N+1 방지)
    @Query("SELECT COUNT(w) FROM WebhookApplication w WHERE w.googleForm.id = :googleFormId AND w.status = :status")
    long countByGoogleFormIdAndStatus(@Param("googleFormId") Long googleFormId, @Param("status") WebhookApplication.ProcessingStatus status);

    // 구글 폼별 + 이메일로 지원서 조회 (Stream 필터링 방지)
    @Query("SELECT w FROM WebhookApplication w WHERE w.googleForm.id = :googleFormId AND w.applicantEmail = :email")
    Optional<WebhookApplication> findByGoogleFormIdAndApplicantEmail(@Param("googleFormId") Long googleFormId, @Param("email") String email);

    // 폼 ID별 + 이메일로 지원서 조회 (Stream 필터링 방지)
    @Query("SELECT w FROM WebhookApplication w WHERE w.googleForm.formId = :formId AND w.applicantEmail = :email ORDER BY w.createdAt DESC")
    Optional<WebhookApplication> findByFormIdAndApplicantEmail(@Param("formId") String formId, @Param("email") String email);

    // 폼 ID별 지원서 개수 조회 (size() 호출 방지)
    @Query("SELECT COUNT(w) FROM WebhookApplication w WHERE w.googleForm.formId = :formId")
    long countByFormId(@Param("formId") String formId);
}