package com.pirogramming.recruit.domain.googleform.event;

import com.pirogramming.recruit.domain.googleform.entity.FormStatus;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.time.LocalDateTime;

/**
 * GoogleForm 관련 이벤트 기본 클래스
 */
@Getter
public abstract class GoogleFormEvent extends ApplicationEvent {
    
    private final Long googleFormId;
    private final String formTitle;
    private final Integer generation;
    private final LocalDateTime occurredAt;

    protected GoogleFormEvent(Object source, Long googleFormId, String formTitle, Integer generation) {
        super(source);
        this.googleFormId = googleFormId;
        this.formTitle = formTitle;
        this.generation = generation;
        this.occurredAt = LocalDateTime.now();
    }
}