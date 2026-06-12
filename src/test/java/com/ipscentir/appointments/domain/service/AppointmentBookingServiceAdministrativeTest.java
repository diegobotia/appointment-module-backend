package com.ipscentir.appointments.domain.service;

import com.ipscentir.appointments.domain.model.appointment.Appointment;
import com.ipscentir.appointments.domain.model.appointment.AppointmentStatus;
import com.ipscentir.appointments.domain.model.appointment.AppointmentType;
import com.ipscentir.appointments.domain.model.facility.FacilityMasterData;
import com.ipscentir.appointments.domain.model.schedule.Schedule;
import com.ipscentir.appointments.infrastructure.persistence.jpa.AppointmentJpaRepository;
import com.ipscentir.appointments.infrastructure.persistence.jpa.ScheduleJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class AppointmentBookingServiceAdministrativeTest {

    @Autowired
    private AppointmentBookingService appointmentBookingService;

    @Autowired
    private AppointmentJpaRepository appointmentJpaRepository;

    @Autowired
    private ScheduleJpaRepository scheduleJpaRepository;

    private Integer sedeId;
    private LocalDate date;
    private LocalTime time;

    @BeforeEach
    void setUp() {
        appointmentJpaRepository.deleteAll();
        scheduleJpaRepository.deleteAll();

        sedeId = FacilityMasterData.SEDE_ID_BELEN;
        date = LocalDate.now().plusDays(1);
        while (date.getDayOfWeek() == java.time.DayOfWeek.SATURDAY
                || date.getDayOfWeek() == java.time.DayOfWeek.SUNDAY) {
            date = date.plusDays(1);
        }
        time = LocalTime.of(9, 30);
    }

    @Test
    void bookAdministrativeAppointmentPersistsWithoutPatient() {
        String primary = UUID.randomUUID().toString();
        String secondary = UUID.randomUUID().toString();
        seedSchedule(primary);
        seedSchedule(secondary);

        Appointment saved = appointmentBookingService.bookAdministrativeAppointment(
                new AdministrativeAppointmentBookingRequest(
                        List.of(primary, secondary),
                        sedeId,
                        date,
                        time,
                        45,
                        "Planificación trimestral"
                )
        );

        assertNull(saved.getPatientId());
        assertEquals(AppointmentType.STAFF, saved.getAppointmentType());
        assertEquals(AppointmentStatus.SCHEDULED, saved.getStatus());
        assertEquals(primary, saved.getDoctorId());
        assertEquals(List.of(secondary), saved.getAdditionalDoctorIds());
        assertTrue(saved.isAdministrative());
        assertEquals(2, saved.getParticipants().size());
    }

    private void seedSchedule(String doctorId) {
        scheduleJpaRepository.save(Schedule.builder()
                .doctorId(doctorId)
                .sedeId(sedeId)
                .specialty("Staff")
                .dayOfWeek(date.getDayOfWeek())
                .startTime(LocalTime.of(7, 0))
                .endTime(LocalTime.of(18, 0))
                .slotDurationMinutes(30)
                .maxPatientsPerSlot(1)
                .isActive(true)
                .build());
    }
}
