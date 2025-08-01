package com.pirogramming.recruit.domain.recruitment.service;

import com.pirogramming.recruit.domain.recruitment.entity.Recruitment;
import com.pirogramming.recruit.domain.recruitment.repository.RecruitmentRepository;
import com.pirogramming.recruit.global.exception.code.ErrorCode;
import com.pirogramming.recruit.global.exception.entity_exception.MemberNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class RecruitmentService {

    private final RecruitmentRepository recruitmentRepository;

    // 현재 모집 중인 리크루팅 조회
    public Optional<Recruitment> getCurrentActiveRecruitment() {
        return recruitmentRepository.findByIsActiveTrue();
    }

    // 현재 모집 중인 리크루팅 조회(필수)
    public Recruitment getCurrentActiveRecruitmentRequired() {
        return getCurrentActiveRecruitment()
                .orElseThrow(() -> new RuntimeException("현재 모집 중인 리크루팅이 없습니다."));
    }

    // ID로 리크루팅 조회
    public Optional<Recruitment> getRecruitmentById(Long id) {
        return recruitmentRepository.findById(id);
    }

    // ID로 리크루팅 조회 (필수)
    public Recruitment getRecruitmentByIdRequired(Long id) {
        return getRecruitmentById(id)
                .orElseThrow(() -> new MemberNotFoundException());
    }

    // 전체 리크루팅 목록 조회
    public List<Recruitment> getAllRecruitments() {
        return recruitmentRepository.findAllOrderByCreatedAtDesc();
    }

    // 특정 기간 리크루팅 조회
    public List<Recruitment> getActiveRecruitmentsByDate() {
        return recruitmentRepository.findActiveRecruitmentsByDate(LocalDate.now());
    }

    // 상태별 리크루팅 조회
    public List<Recruitment> getRecruitmentsByStatus(Recruitment.RecruitmentStatus status) {
        return recruitmentRepository.findByStatus(status);
    }

    // 새 리크루팅 생성
    @Transactional
    public Recruitment createRecruitment(Recruitment recruitment) {
        log.info("새 리크루팅 생성: {}", recruitment.getTitle());
        return recruitmentRepository.save(recruitment);
    }

    // 리크루팅 활성화 (기존 활성화된 것은 비활성화)
    @Transactional
    public Recruitment activateRecruitment(Long recruitmentId) {
        log.info("리크루팅 활성화: {}", recruitmentId);

        // 기존 활성화된 리크루팅 비활성화
        recruitmentRepository.findByIsActiveTrue()
                .ifPresent(activeRecruitment -> {
                    activeRecruitment.deactivate();
                    recruitmentRepository.save(activeRecruitment);
                    log.info("기존 활성화된 리크루팅 비활성화: {}", activeRecruitment.getId());
                });

        // 새 리크루팅 활성화
        Recruitment recruitment = getRecruitmentByIdRequired(recruitmentId);
        recruitment.activate();

        return recruitmentRepository.save(recruitment);
    }

    // 리크루팅 완료 처리
    @Transactional
    public Recruitment completeRecruitment(Long recruitmentId) {
        log.info("리크루팅 완료 처리: {}", recruitmentId);

        Recruitment recruitment = getRecruitmentByIdRequired(recruitmentId);
        recruitment.complete();

        return recruitmentRepository.save(recruitment);
    }

    // Google Form URL 업데이트
    @Transactional
    public Recruitment updateGoogleFormUrl(Long recruitmentId, String newUrl) {
        log.info("리크루팅 {} 구글폼 URL 업데이트: {}", recruitmentId, newUrl);

        Recruitment recruitment = getRecruitmentByIdRequired(recruitmentId);
        recruitment.updateGoogleFormUrl(newUrl);

        return recruitmentRepository.save(recruitment);
    }
}