package com.ipscentir.appointments.domain.service;

import com.ipscentir.appointments.domain.model.appointment.AppointmentType;
import com.ipscentir.appointments.domain.model.catalog.AppointmentServiceType;
import com.ipscentir.appointments.domain.model.facility.FacilityResourceType;
import com.ipscentir.appointments.domain.model.schedule.Schedule;

import static com.ipscentir.appointments.domain.model.appointment.AppointmentType.BLOQUEO;
import static com.ipscentir.appointments.domain.model.appointment.AppointmentType.JUNTA_MEDICA;
import static com.ipscentir.appointments.domain.model.appointment.AppointmentType.PRESENCIAL;
import static com.ipscentir.appointments.domain.model.appointment.AppointmentType.STAFF;
import static com.ipscentir.appointments.domain.model.appointment.AppointmentType.TERAPIA_FISICA;
import static com.ipscentir.appointments.domain.model.appointment.AppointmentType.TERAPIA_OCUPACIONAL;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

/**
 * Matriz cerrada: tipo de servicio del especialista ↔ recurso físico ↔ tipo de cita.
 */
public final class ServiceResourceMatrix {

    private static final Map<AppointmentServiceType, FacilityResourceType> SERVICE_TO_RESOURCE;

    static {
        EnumMap<AppointmentServiceType, FacilityResourceType> map = new EnumMap<>(AppointmentServiceType.class);
        map.put(AppointmentServiceType.TERAPIA_FISICA, FacilityResourceType.FISIOTERAPIA);
        map.put(AppointmentServiceType.TERAPIA_OCUPACIONAL, FacilityResourceType.TERAPIA_OCUPACIONAL);
        map.put(AppointmentServiceType.JUNTA_MEDICA, FacilityResourceType.CONSULTORIO);
        map.put(AppointmentServiceType.STAFF, FacilityResourceType.REUNION_STAFF);
        for (AppointmentServiceType type : AppointmentServiceType.values()) {
            map.putIfAbsent(type, FacilityResourceType.CONSULTORIO);
        }
        SERVICE_TO_RESOURCE = Map.copyOf(map);
    }

    private ServiceResourceMatrix() {
    }

    public static FacilityResourceType resourceForServiceType(AppointmentServiceType serviceType) {
        return SERVICE_TO_RESOURCE.getOrDefault(serviceType, FacilityResourceType.CONSULTORIO);
    }

    public static AppointmentType appointmentTypeForService(AppointmentServiceType serviceType) {
        return switch (serviceType) {
            case TERAPIA_FISICA -> TERAPIA_FISICA;
            case TERAPIA_OCUPACIONAL -> TERAPIA_OCUPACIONAL;
            case JUNTA_MEDICA -> JUNTA_MEDICA;
            case STAFF -> STAFF;
            default -> PRESENCIAL;
        };
    }

    public static boolean isBloqueo(AppointmentServiceType serviceType) {
        return serviceType == AppointmentServiceType.MEDICO_DOLOR
                || serviceType == AppointmentServiceType.MEDICO_FISIATRA_INTEGRAL_DOLOR;
    }

    public static FacilityResourceType resourceForAppointmentType(AppointmentType appointmentType) {
        return AppointmentResourceTypeResolver.forAppointmentType(appointmentType);
    }

    public static void assertScheduleAlignsWithAppointmentType(Schedule schedule, AppointmentType appointmentType) {
        if (appointmentType == AppointmentType.BLOQUEO) {
            return;
        }

        Optional<AppointmentServiceType> scheduleService = AppointmentServiceType.tryResolve(schedule.getSpecialty());
        FacilityResourceType requiredResource = resourceForAppointmentType(appointmentType);

        if (scheduleService.isEmpty()) {
            if (appointmentType == AppointmentType.TERAPIA_FISICA
                    || appointmentType == AppointmentType.TERAPIA_OCUPACIONAL) {
                throw new IllegalStateException(
                        "La agenda del especialista no indica un servicio de terapia compatible con "
                                + appointmentType
                );
            }
            return;
        }

        AppointmentServiceType resolved = scheduleService.get();
        FacilityResourceType scheduleResource = resourceForServiceType(resolved);

        if (scheduleResource != requiredResource) {
            throw new IllegalStateException(
                    "El especialista (%s) atiende en %s pero la cita requiere %s"
                            .formatted(
                                    resolved.getDisplayName(),
                                    label(scheduleResource),
                                    label(requiredResource)
                            )
            );
        }

        switch (appointmentType) {
            case TERAPIA_FISICA -> requireService(resolved, AppointmentServiceType.TERAPIA_FISICA);
            case TERAPIA_OCUPACIONAL -> requireService(resolved, AppointmentServiceType.TERAPIA_OCUPACIONAL);
            case JUNTA_MEDICA -> requireService(resolved, AppointmentServiceType.JUNTA_MEDICA);
            case STAFF -> requireService(resolved, AppointmentServiceType.STAFF);
            case PRESENCIAL -> {
                if (isTherapyService(resolved)) {
                    throw new IllegalStateException(
                            "La agenda de terapia no admite citas presenciales de consultorio"
                    );
                }
            }
            case BLOQUEO -> {}
        }
    }

    private static void requireService(AppointmentServiceType actual, AppointmentServiceType expected) {
        if (actual != expected) {
            throw new IllegalStateException(
                    "La especialidad de la agenda (%s) no coincide con el tipo de cita (%s)"
                            .formatted(actual.getDisplayName(), expected.name())
            );
        }
    }

    private static boolean isTherapyService(AppointmentServiceType type) {
        return type == AppointmentServiceType.TERAPIA_FISICA
                || type == AppointmentServiceType.TERAPIA_OCUPACIONAL;
    }

    private static String label(FacilityResourceType type) {
        return switch (type) {
            case CONSULTORIO -> "consultorio";
            case FISIOTERAPIA -> "sala de fisioterapia";
            case TERAPIA_OCUPACIONAL -> "sala de terapia ocupacional";
            case REUNION_STAFF -> "sala de reunión";
            case SIN_RECURSO -> "sin recurso físico";
        };
    }
}
