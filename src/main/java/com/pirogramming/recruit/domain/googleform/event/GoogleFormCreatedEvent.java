package com.pirogramming.recruit.domain.googleform.event;

/**
 * GoogleForm 생성 이벤트
 */
public class GoogleFormCreatedEvent extends GoogleFormEvent {

    public GoogleFormCreatedEvent(Object source, Long googleFormId, String formTitle, Integer generation) {
        super(source, googleFormId, formTitle, generation);
    }
}