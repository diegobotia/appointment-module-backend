package com.ipscentir.appointments.application.service;

public enum TherapyUnderMinPolicy {
    CANCEL,
    RESCHEDULE,
    FORCE_CONFIRMATION,
    KEEP_PENDING;

    public static TherapyUnderMinPolicy fromConfig(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return RESCHEDULE;
        }

        return switch (rawValue.trim().toUpperCase()) {
            case "CANCEL" -> CANCEL;
            case "RESCHEDULE" -> RESCHEDULE;
            case "FORCE_CONFIRMATION" -> FORCE_CONFIRMATION;
            case "KEEP_PENDING" -> KEEP_PENDING;
            default -> throw new IllegalArgumentException("Unsupported therapy under-min policy: " + rawValue);
        };
    }
}
