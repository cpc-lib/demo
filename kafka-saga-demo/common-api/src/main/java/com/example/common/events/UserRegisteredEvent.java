package com.example.common.events;

import lombok.Data;

@Data
public class UserRegisteredEvent {
    private String eventId;
    private Long userId;
    private String username;
    private String phone;
    private Integer initPoints;
}
