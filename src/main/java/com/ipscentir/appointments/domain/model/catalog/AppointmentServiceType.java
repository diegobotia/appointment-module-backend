package com.ipscentir.appointments.domain.model.catalog;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.text.Normalizer;
import java.util.Arrays;

import static java.util.Locale.ROOT;

@Getter
@RequiredArgsConstructor
public enum AppointmentServiceType {
    MEDICO_FISIATRIA("Medico fisiatria"),
    MEDICO_FISIATRA_INTEGRAL_DOLOR("Medico fisiatra integral del dolor"),
    MEDICO_DOLOR("Medico de dolor"),
    MEDICO_LABORAL("Medico laboral"),
    PSICOLOGIA("Psicologia"),
    MEDICO_PSIQUIATRA("Medico psiquiatra"),
    ELECTROMIOGRAFIA("Electromiografia"),
    TERAPIA_FISICA("Terapia fisica"),
    TERAPIA_OCUPACIONAL("Terapia ocupacional"),
    JUNTA_MEDICA("Junta medica"),
    STAFF("Staff");

    private final String displayName;

    public static AppointmentServiceType fromFlexibleValue(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("serviceType is required");
        }

        return Arrays.stream(values())
                .filter(type -> type.matches(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown serviceType: " + value));
    }

    public boolean matches(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }

        String normalizedIncoming = normalize(value);
        return normalize(name()).equals(normalizedIncoming)
                || normalize(displayName).equals(normalizedIncoming);
    }

    private static String normalize(String value) {
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return normalized.trim().toLowerCase(ROOT)
                .replaceAll("[^a-z0-9]+", "");
    }
}
