package com.example.ai.controller.dto;

import java.io.Serializable;

public record ImageRequest(String prompt, String size) implements Serializable {}
