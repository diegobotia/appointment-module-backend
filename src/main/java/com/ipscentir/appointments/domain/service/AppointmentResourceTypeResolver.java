package com.ipscentir.appointments.domain.service;

import com.ipscentir.appointments.domain.model.appointment.AppointmentType;
import com.ipscentir.appointments.domain.model.catalog.AppointmentServiceType;
import com.ipscentir.appointments.domain.model.facility.FacilityResourceType;

/**
 * Mapea tipos de cita / servicio al recurso físico que consumen.
 */
public final class AppointmentResourceTypeResolver {

    private AppointmentResourceTypeResolver() {
    }

    public static FacilityResourceType forAppointmentType(AppointmentType type) {
        return switch (type) {
            case TERAPIA_FISICA -> FacilityResourceType.FISIOTERAPIA;
            case TERAPIA_OCUPACIONAL -> FacilityResourceType.TERAPIA_OCUPACIONAL;
            case PRESENCIAL, JUNTA_MEDICA -> FacilityResourceType.CONSULTORIO;
        };
    }

    public static FacilityResourceType forServiceType(AppointmentServiceType serviceType) {
        return switch (serviceType) {
            case TERAPIA_FISICA -> FacilityResourceType.FISIOTERAPIA;
            case TERAPIA_OCUPACIONAL -> FacilityResourceType.TERAPIA_OCUPACIONAL;
            case JUNTA_MEDICA -> FacilityResourceType.CONSULTORIO;
            default -> FacilityResourceType.CONSULTORIO;
        };
    }
}
