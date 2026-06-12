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
            case STAFF -> FacilityResourceType.REUNION_STAFF;
            case PRESENCIAL, JUNTA_MEDICA -> FacilityResourceType.CONSULTORIO;
            case BLOQUEO -> FacilityResourceType.SIN_RECURSO;
        };
    }

    public static FacilityResourceType forServiceType(AppointmentServiceType serviceType) {
        return ServiceResourceMatrix.resourceForServiceType(serviceType);
    }
}
