package com.ipscentir.appointments.domain.model.catalog;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

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
}
