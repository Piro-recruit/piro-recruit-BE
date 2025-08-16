package com.pirogramming.recruit.domain.googleform.service;

import com.pirogramming.recruit.domain.googleform.entity.GoogleForm;
import com.pirogramming.recruit.domain.googleform.repository.GoogleFormRepository;
import com.pirogramming.recruit.global.exception.code.ErrorCode;
import com.pirogramming.recruit.global.exception.entity_exception.DuplicateResourceException;
import com.pirogramming.recruit.global.exception.RecruitException;
import org.springframework.http.HttpStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class GoogleFormService {

    private final GoogleFormRepository googleFormRepository;

    // 현재 활성화된 구글 폼 조회
    public Optional<GoogleForm> getActiveGoogleForm() {
        return googleFormRepository.findByIsActiveTrue();
    }

    // 현재 활성화된 구글 폼 조회(필수)
    public GoogleForm getActiveGoogleFormRequired() {
        return getActiveGoogleForm()
                .orElseThrow(() -> new RecruitException(HttpStatus.NOT_FOUND, ErrorCode.GOOGLE_FORM_NOT_ACTIVE));
    }

    // ID로 구글 폼 조회
    public Optional<GoogleForm> getGoogleFormById(Long id) {
        return googleFormRepository.findById(id);
    }

    // ID로 구글 폼 조회 (필수)
    public GoogleForm getGoogleFormByIdRequired(Long id) {
        return getGoogleFormById(id)
                .orElseThrow(() -> new RecruitException(HttpStatus.NOT_FOUND, ErrorCode.GOOGLE_FORM_NOT_FOUND));
    }

    // 폼 ID로 구글 폼 조회
    public Optional<GoogleForm> getGoogleFormByFormId(String formId) {
        return googleFormRepository.findByFormId(formId);
    }

    // 폼 ID로 구글 폼 조회 (필수)
    public GoogleForm getGoogleFormByFormIdRequired(String formId) {
        return getGoogleFormByFormId(formId)
                .orElseThrow(() -> new RecruitException(HttpStatus.NOT_FOUND, ErrorCode.GOOGLE_FORM_NOT_FOUND));
    }

    // 전체 구글 폼 목록 조회
    public List<GoogleForm> getAllGoogleForms() {
        return googleFormRepository.findAllOrderByCreatedAtDesc();
    }


    // 새 구글 폼 생성
    @Transactional
    public GoogleForm createGoogleForm(GoogleForm googleForm) {
        log.info("새 구글 폼 생성: {} ({}기)", googleForm.getTitle(), googleForm.getGeneration());

        // 중복 검사
        if (googleFormRepository.existsByFormId(googleForm.getFormId())) {
            throw new DuplicateResourceException("구글 폼 ID", googleForm.getFormId());
        }

        return googleFormRepository.save(googleForm);
    }

    // 구글 폼 활성화 (기존 활성화된 것은 비활성화)
    @Transactional
    public GoogleForm activateGoogleForm(Long googleFormId) {
        log.info("구글 폼 활성화: {}", googleFormId);

        // 대상 구글 폼 존재 확인
        GoogleForm targetGoogleForm = getGoogleFormByIdRequired(googleFormId);
        
        // 이미 활성화되어 있다면 그대로 반환
        if (Boolean.TRUE.equals(targetGoogleForm.getIsActive())) {
            log.info("구글 폼이 이미 활성화되어 있음: {}", googleFormId);
            return targetGoogleForm;
        }

        // 모든 구글 폼 비활성화 (원자적 일괄 업데이트)
        int deactivatedCount = googleFormRepository.deactivateAllGoogleForms();
        log.info("기존 활성화된 구글 폼 {}개 비활성화 완료", deactivatedCount);

        // 대상 구글 폼 활성화
        targetGoogleForm.activate();
        GoogleForm savedGoogleForm = googleFormRepository.save(targetGoogleForm);
        
        log.info("구글 폼 활성화 완료: {}", googleFormId);
        return savedGoogleForm;
    }

    // 구글 폼 비활성화
    @Transactional
    public GoogleForm deactivateGoogleForm(Long googleFormId) {
        log.info("구글 폼 비활성화: {}", googleFormId);

        GoogleForm googleForm = getGoogleFormByIdRequired(googleFormId);
        googleForm.deactivate();

        return googleFormRepository.save(googleForm);
    }

    // 구글 폼 URL 업데이트
    @Transactional
    public GoogleForm updateGoogleFormUrl(Long googleFormId, String newUrl) {
        log.info("구글 폼 {} URL 업데이트: {}", googleFormId, newUrl);

        GoogleForm googleForm = getGoogleFormByIdRequired(googleFormId);
        googleForm.updateFormUrl(newUrl);

        return googleFormRepository.save(googleForm);
    }

    // 구글 시트 URL 업데이트
    @Transactional
    public GoogleForm updateGoogleSheetUrl(Long googleFormId, String newUrl) {
        log.info("구글 폼 {} 시트 URL 업데이트: {}", googleFormId, newUrl);

        GoogleForm googleForm = getGoogleFormByIdRequired(googleFormId);
        googleForm.updateSheetUrl(newUrl);

        return googleFormRepository.save(googleForm);
    }

    // 제목으로 구글 폼 검색 (대소문자 무시)
    public List<GoogleForm> searchGoogleFormsByTitle(String title) {
        return googleFormRepository.findByTitleContainingIgnoreCase(title);
    }

    // 구글 폼 삭제
    @Transactional
    public void deleteGoogleForm(Long googleFormId) {
        log.info("구글 폼 삭제: {}", googleFormId);

        GoogleForm googleForm = getGoogleFormByIdRequired(googleFormId);

        // 활성화된 폼인지 확인
        if (Boolean.TRUE.equals(googleForm.getIsActive())) {
            log.warn("활성화된 구글 폼 삭제 시도: {}", googleFormId);
            throw new RecruitException(HttpStatus.BAD_REQUEST, ErrorCode.GOOGLE_FORM_ACTIVE_CANNOT_DELETE);
        }

        googleFormRepository.delete(googleForm);
        log.info("구글 폼 삭제 완료: {}", googleFormId);
    }

    // 특정 기수의 구글 폼들 조회
    public List<GoogleForm> getGoogleFormsByGeneration(Integer generation) {
        return googleFormRepository.findByGenerationOrderByCreatedAtDesc(generation);
    }

    // 특정 기수의 활성화된 구글 폼 조회
    public Optional<GoogleForm> getActiveGoogleFormByGeneration(Integer generation) {
        return googleFormRepository.findByGenerationAndIsActiveTrue(generation);
    }

    // 특정 기수의 활성화된 구글 폼 조회 (필수)
    public GoogleForm getActiveGoogleFormByGenerationRequired(Integer generation) {
        return getActiveGoogleFormByGeneration(generation)
                .orElseThrow(() -> new RecruitException(HttpStatus.NOT_FOUND,
                        ErrorCode.GOOGLE_FORM_NOT_FOUND));
    }

    // 현재 활성화된 기수 조회
    public Optional<Integer> getCurrentActiveGeneration() {
        return googleFormRepository.findCurrentActiveGeneration();
    }

    // 현재 활성화된 기수 조회 (필수)
    public Integer getCurrentActiveGenerationRequired() {
        return getCurrentActiveGeneration()
                .orElseThrow(() -> new RecruitException(HttpStatus.NOT_FOUND,
                        ErrorCode.GOOGLE_FORM_NOT_ACTIVE));
    }

    // 가장 최신 기수 조회
    public Optional<Integer> getLatestGeneration() {
        return googleFormRepository.findMaxGeneration();
    }

    // 특정 기수가 존재하는지 확인
    public boolean existsByGeneration(Integer generation) {
        return googleFormRepository.existsByGeneration(generation);
    }

    // 기수 범위로 구글 폼 조회
    public List<GoogleForm> getGoogleFormsByGenerationRange(Integer startGeneration, Integer endGeneration) {
        return googleFormRepository.findByGenerationBetweenOrderByGenerationDescCreatedAtDesc(
                startGeneration, endGeneration);
    }

    // 기수 업데이트
    @Transactional
    public GoogleForm updateGoogleFormGeneration(Long googleFormId, Integer newGeneration) {
        log.info("구글 폼 {} 기수 업데이트: {}", googleFormId, newGeneration);

        GoogleForm googleForm = getGoogleFormByIdRequired(googleFormId);
        googleForm.updateGeneration(newGeneration);

        return googleFormRepository.save(googleForm);
    }
}