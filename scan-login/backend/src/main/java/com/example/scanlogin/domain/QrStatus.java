package com.example.scanlogin.domain;

/** Public QR lifecycle. Error results (FORBIDDEN, ALREADY_CONSUMED) are not lifecycle states. */
public enum QrStatus {
    WAIT_SCAN, SCANNED, CONFIRMED, CONSUMED, EXPIRED;

    public static QrStatus fromStored(String raw) {
        if (raw == null) return EXPIRED;
        return QrStatus.valueOf(raw);
    }
}
