package com.pirogramming.recruit.domain.googleform.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * GoogleForm 이벤트 처리기
 * 
 * 각 이벤트에 대해 부가적인 처리를 수행합니다:
 * - 로깅 및 모니터링
 * - 알림 발송
 * - 통계 업데이트
 * - 외부 시스템 연동
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GoogleFormEventHandler {

    /**
     * GoogleForm 생성 이벤트 처리
     */
    @EventListener
    @Async("googleFormEventExecutor")
    public void handleGoogleFormCreated(GoogleFormCreatedEvent event) {
        log.info("[이벤트] GoogleForm 생성됨 - ID: {}, 제목: {}, 기수: {}기", 
            event.getGoogleFormId(), event.getFormTitle(), event.getGeneration());
        
        // TODO: 추가 처리
        // - 슬랙/디스코드 알림 발송
        // - 생성 통계 업데이트
        // - 관리자에게 알림 이메일 발송
        
        logEventDetails("CREATED", event);
    }

    /**
     * GoogleForm 활성화 이벤트 처리
     */
    @EventListener
    @Async("googleFormEventExecutor")
    public void handleGoogleFormActivated(GoogleFormActivatedEvent event) {
        log.info("[이벤트] GoogleForm 활성화됨 - ID: {}, 제목: {}, 기수: {}기", 
            event.getGoogleFormId(), event.getFormTitle(), event.getGeneration());
        
        // TODO: 추가 처리
        // - 리쿠르팅 시작 알림 발송
        // - 홈페이지 배너 업데이트
        // - SNS 공지사항 게시
        // - 관련 서비스들에게 알림 (AI 배치 처리 등)
        
        logEventDetails("ACTIVATED", event);
    }

    /**
     * GoogleForm 마감 이벤트 처리
     */
    @EventListener
    @Async("googleFormEventExecutor")
    public void handleGoogleFormClosed(GoogleFormClosedEvent event) {
        log.info("[이벤트] GoogleForm 마감됨 - ID: {}, 제목: {}, 기수: {}기", 
            event.getGoogleFormId(), event.getFormTitle(), event.getGeneration());
        
        // TODO: 추가 처리
        // - 마감 알림 발송
        // - 최종 통계 생성
        // - 지원자들에게 마감 안내 이메일
        // - 관련 배치 작업 중단
        
        logEventDetails("CLOSED", event);
    }

    /**
     * GoogleForm 상태 변경 이벤트 처리
     */
    @EventListener
    @Async("googleFormEventExecutor")
    public void handleGoogleFormStatusChanged(GoogleFormStatusChangedEvent event) {
        log.info("[이벤트] GoogleForm 상태 변경됨 - ID: {}, {} -> {}, 사유: {}", 
            event.getGoogleFormId(), 
            event.getFromStatus().getDescription(), 
            event.getToStatus().getDescription(), 
            event.getReason());
        
        // TODO: 추가 처리
        // - 상태 변경 이력 DB 저장
        // - 모니터링 메트릭 업데이트
        // - 외부 시스템에 상태 동기화
        
        logStatusChangeDetails(event);
    }

    /**
     * 이벤트 상세 정보 로깅
     */
    private void logEventDetails(String eventType, GoogleFormEvent event) {
        log.debug("[이벤트 상세] Type: {}, ID: {}, Title: {}, Generation: {}, Time: {}", 
            eventType, event.getGoogleFormId(), event.getFormTitle(), 
            event.getGeneration(), event.getOccurredAt());
    }

    /**
     * 상태 변경 이벤트 상세 정보 로깅
     */
    private void logStatusChangeDetails(GoogleFormStatusChangedEvent event) {
        log.debug("[상태 변경 상세] ID: {}, From: {} -> To: {}, Reason: {}, Time: {}", 
            event.getGoogleFormId(), 
            event.getFromStatus(), event.getToStatus(), 
            event.getReason(), event.getOccurredAt());
    }
}