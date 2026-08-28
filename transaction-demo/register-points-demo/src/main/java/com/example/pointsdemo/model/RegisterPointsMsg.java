package com.example.pointsdemo.model;

import lombok.Data;

@Data
public class RegisterPointsMsg {

    private Long userId;

    private String businessId;

    private Integer points;
}