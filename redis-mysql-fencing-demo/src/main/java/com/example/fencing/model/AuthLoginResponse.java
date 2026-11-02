package com.example.fencing.model;

public record AuthLoginResponse(String token, String userId, long expiresInMs) { }
