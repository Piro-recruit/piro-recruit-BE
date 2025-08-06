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

    // 활성화된 구글 폼 목록 조회
    public List<GoogleForm> getActiveGoogleForms() {
        return googleFormRepository.findByIsActiveTrueOrderByCreatedAtDesc();
    }

    // 새 구글 폼 생성
    @Transactional
    public GoogleForm createGoogleForm(GoogleForm googleForm) {
        log.info("새 구글 폼 생성: {}", googleForm.getTitle());

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

        // 기존 활성화된 구글 폼 비활성화
        googleFormRepository.findByIsActiveTrue()
                .ifPresent(activeForm -> {
                    activeForm.deactivate();
                    googleFormRepository.save(activeForm);
                    log.info("기존 활성화된 구글 폼 비활성화: {}", activeForm.getId());
                });

        // 새 구글 폼 활성화
        GoogleForm googleForm = getGoogleFormByIdRequired(googleFormId);
        googleForm.activate();

        return googleFormRepository.save(googleForm);
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
}