package com.example.common.events;

import lombok.Data;

@Data
public class UserRegisterCompensateEvent {
    private String eventId;
    private Long userId;
    private String reason;
}
