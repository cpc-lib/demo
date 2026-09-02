package com.example.sha256.common.model;

public enum TaskStatus {
    QUEUED,
    RUNNING,
    RETRYING,
    SUCCESS,
    FAILED,
    DEAD_LETTERED
}
