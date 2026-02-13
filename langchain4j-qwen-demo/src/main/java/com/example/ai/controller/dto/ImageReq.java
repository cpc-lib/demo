package com.example.ai.controller.dto;

import java.io.Serializable;

public record ImageReq(String prompt, String size, String negativePrompt) implements Serializable {
}
