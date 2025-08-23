package com.pirogramming.recruit.domain.googleform.event;

import com.pirogramming.recruit.domain.googleform.entity.FormStatus;
import com.pirogramming.recruit.domain.googleform.entity.GoogleForm;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * GoogleForm 이벤트 발행기
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GoogleFormEventPublisher {

    private final ApplicationEventPublisher eventPublisher;

    /**
     * GoogleForm 생성 이벤트 발행
     */
    public void publishCreated(GoogleForm googleForm) {
        GoogleFormCreatedEvent event = new GoogleFormCreatedEvent(
            this, googleForm.getId(), googleForm.getTitle(), googleForm.getGeneration()
        );
        
        log.info("GoogleForm 생성 이벤트 발행: ID={}, Title={}", googleForm.getId(), googleForm.getTitle());
        eventPublisher.publishEvent(event);
    }

    /**
     * GoogleForm 활성화 이벤트 발행
     */
    public void publishActivated(GoogleForm googleForm) {
        GoogleFormActivatedEvent event = new GoogleFormActivatedEvent(
            this, googleForm.getId(), googleForm.getTitle(), googleForm.getGeneration()
        );
        
        log.info("GoogleForm 활성화 이벤트 발행: ID={}, Title={}", googleForm.getId(), googleForm.getTitle());
        eventPublisher.publishEvent(event);
    }

    /**
     * GoogleForm 마감 이벤트 발행
     */
    public void publishClosed(GoogleForm googleForm) {
        GoogleFormClosedEvent event = new GoogleFormClosedEvent(
            this, googleForm.getId(), googleForm.getTitle(), googleForm.getGeneration()
        );
        
        log.info("GoogleForm 마감 이벤트 발행: ID={}, Title={}", googleForm.getId(), googleForm.getTitle());
        eventPublisher.publishEvent(event);
    }

    /**
     * GoogleForm 상태 변경 이벤트 발행
     */
    public void publishStatusChanged(GoogleForm googleForm, FormStatus fromStatus, FormStatus toStatus, String reason) {
        GoogleFormStatusChangedEvent event = new GoogleFormStatusChangedEvent(
            this, googleForm.getId(), googleForm.getTitle(), googleForm.getGeneration(),
            fromStatus, toStatus, reason
        );
        
        log.info("GoogleForm 상태 변경 이벤트 발행: ID={}, {} -> {}, Reason={}", 
            googleForm.getId(), fromStatus, toStatus, reason);
        eventPublisher.publishEvent(event);
    }
}