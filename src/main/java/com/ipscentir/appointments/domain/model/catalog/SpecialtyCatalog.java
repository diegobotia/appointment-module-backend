package com.ipscentir.appointments.domain.model.catalog;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SpecialtyCatalog {
    FISIATRIA("Fisiatria"),
    FISIATRIA_INTEGRAL_DOLOR("Fisiatria integral del dolor"),
    DOLOR("Dolor"),
    MEDICINA_LABORAL("Medicina laboral"),
    PSICOLOGIA("Psicologia"),
    PSIQUIATRIA("Psiquiatria"),
    ELECTROMIOGRAFIA("Electromiografia"),
    TERAPIA_FISICA("Terapia fisica"),
    TERAPIA_OCUPACIONAL("Terapia ocupacional");

    private final String displayName;
}
