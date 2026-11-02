package com.example.fencing.model;

public record CounterSnapshot(String resourceId, long value, long fenceToken) { }
