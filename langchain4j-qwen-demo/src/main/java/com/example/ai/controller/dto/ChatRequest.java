package com.example.ai.controller.dto;

import java.io.Serializable;

public record ChatRequest(String sessionId, String message) implements Serializable {}
