package com.example.orderjob.domain;

public record OrderCloseSummary(
        int scanned,
        int closed,
        int alreadyHandled,
        int notExpired,
        int notFound,
        int pages,
        boolean truncated
) {
    public String toMessage() {
        return "scanned=" + scanned
                + ", closed=" + closed
                + ", alreadyHandled=" + alreadyHandled
                + ", notExpired=" + notExpired
                + ", notFound=" + notFound
                + ", pages=" + pages
                + ", truncated=" + truncated;
    }
}
