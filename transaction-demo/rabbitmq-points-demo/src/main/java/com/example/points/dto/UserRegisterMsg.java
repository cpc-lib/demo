package com.example.points.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserRegisterMsg implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long userId;
    private String businessId;
}
