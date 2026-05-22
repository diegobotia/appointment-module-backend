package com.ipscentir.appointments.domain.service;

import com.ipscentir.appointments.domain.model.facility.FacilityMasterData;

import com.ipscentir.appointments.domain.model.appointment.AppointmentType;
import com.ipscentir.appointments.domain.model.catalog.AppointmentServiceType;
import com.ipscentir.appointments.domain.model.facility.FacilityResourceType;
import com.ipscentir.appointments.domain.model.schedule.Schedule;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ServiceResourceMatrixTest {

    @Test
    void mapsTherapyServicesToCorrectRooms() {
        assertEquals(
                FacilityResourceType.FISIOTERAPIA,
                ServiceResourceMatrix.resourceForServiceType(AppointmentServiceType.TERAPIA_FISICA)
        );
        assertEquals(
                FacilityResourceType.TERAPIA_OCUPACIONAL,
                ServiceResourceMatrix.resourceForServiceType(AppointmentServiceType.TERAPIA_OCUPACIONAL)
        );
    }

    @Test
    void mapsMedicalSpecialtiesToConsultorio() {
        assertEquals(
                FacilityResourceType.CONSULTORIO,
                ServiceResourceMatrix.resourceForServiceType(AppointmentServiceType.MEDICO_FISIATRIA)
        );
    }

    @Test
    void rejectsPresencialOnTherapySchedule() {
        Schedule schedule = therapySchedule(AppointmentServiceType.TERAPIA_FISICA);

        assertThrows(
                IllegalStateException.class,
                () -> ServiceResourceMatrix.assertScheduleAlignsWithAppointmentType(
                        schedule,
                        AppointmentType.PRESENCIAL
                )
        );
    }

    @Test
    void acceptsTherapyAppointmentOnMatchingSchedule() {
        Schedule schedule = therapySchedule(AppointmentServiceType.TERAPIA_FISICA);

        assertDoesNotThrow(() -> ServiceResourceMatrix.assertScheduleAlignsWithAppointmentType(
                schedule,
                AppointmentType.TERAPIA_FISICA
        ));
    }

    @Test
    void rejectsTherapyAppointmentWhenScheduleSpecialtyMismatch() {
        Schedule schedule = therapySchedule(AppointmentServiceType.TERAPIA_OCUPACIONAL);

        assertThrows(
                IllegalStateException.class,
                () -> ServiceResourceMatrix.assertScheduleAlignsWithAppointmentType(
                        schedule,
                        AppointmentType.TERAPIA_FISICA
                )
        );
    }

    private static Schedule therapySchedule(AppointmentServiceType serviceType) {
        return Schedule.builder()
                .id(UUID.randomUUID())
                .doctorId(UUID.randomUUID().toString())
                .sedeId(FacilityMasterData.SEDE_ID_BELEN)
                .dayOfWeek(DayOfWeek.MONDAY)
                .startTime(LocalTime.of(8, 0))
                .endTime(LocalTime.of(12, 0))
                .slotDurationMinutes(30)
                .maxPatientsPerSlot(6)
                .specialty(serviceType.getDisplayName())
                .isActive(true)
                .build();
    }
}
