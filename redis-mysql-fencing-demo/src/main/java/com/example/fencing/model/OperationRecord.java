package com.example.fencing.model;

public record OperationRecord(
        String resourceId, String requestId,
        long delta, long appliedValue, long fenceToken) {
    public MutationResponse toResponse(boolean replayed) {
        return new MutationResponse(resourceId, requestId, delta,
                appliedValue, fenceToken, replayed);
    }
}
