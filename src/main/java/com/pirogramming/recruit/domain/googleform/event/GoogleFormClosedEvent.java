package com.pirogramming.recruit.domain.googleform.event;

/**
 * GoogleForm 마감 이벤트
 */
public class GoogleFormClosedEvent extends GoogleFormEvent {

    public GoogleFormClosedEvent(Object source, Long googleFormId, String formTitle, Integer generation) {
        super(source, googleFormId, formTitle, generation);
    }
}