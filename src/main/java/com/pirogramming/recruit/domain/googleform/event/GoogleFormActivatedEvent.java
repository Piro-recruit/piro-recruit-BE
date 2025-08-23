package com.pirogramming.recruit.domain.googleform.event;

/**
 * GoogleForm 활성화 이벤트
 */
public class GoogleFormActivatedEvent extends GoogleFormEvent {

    public GoogleFormActivatedEvent(Object source, Long googleFormId, String formTitle, Integer generation) {
        super(source, googleFormId, formTitle, generation);
    }
}