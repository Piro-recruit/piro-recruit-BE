package com.pirogramming.recruit.domain.googleform.event;

import com.pirogramming.recruit.domain.googleform.entity.FormStatus;
import lombok.Getter;

/**
 * GoogleForm 상태 변경 이벤트
 */
@Getter
public class GoogleFormStatusChangedEvent extends GoogleFormEvent {
    
    private final FormStatus fromStatus;
    private final FormStatus toStatus;
    private final String reason;

    public GoogleFormStatusChangedEvent(Object source, Long googleFormId, String formTitle, Integer generation,
                                      FormStatus fromStatus, FormStatus toStatus, String reason) {
        super(source, googleFormId, formTitle, generation);
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.reason = reason;
    }
}