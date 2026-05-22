package com.ipscentir.appointments.domain.service;

import com.ipscentir.appointments.application.exception.ResourceCapacityExceededException;
import com.ipscentir.appointments.domain.model.appointment.AppointmentType;
import com.ipscentir.appointments.domain.model.facility.FacilityMasterData;
import com.ipscentir.appointments.domain.model.schedule.Schedule;
import com.ipscentir.appointments.domain.repository.ScheduleRepository;
import com.ipscentir.appointments.infrastructure.persistence.jpa.AppointmentJpaRepository;
import com.ipscentir.appointments.infrastructure.persistence.jpa.AppointmentResourceAllocationJpaRepository;
import com.ipscentir.appointments.infrastructure.persistence.jpa.SedeJpaRepository;
import com.ipscentir.appointments.infrastructure.persistence.jpa.ScheduleJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("test")
class ResourceCapacityIntegrationTest {

    @Autowired
    private AppointmentBookingService appointmentBookingService;

    @Autowired
    private ScheduleRepository scheduleRepository;

    @Autowired
    private ScheduleJpaRepository scheduleJpaRepository;

    @Autowired
    private AppointmentJpaRepository appointmentJpaRepository;

    @Autowired
    private AppointmentResourceAllocationJpaRepository allocationJpaRepository;

    @Autowired
    private SedeJpaRepository sedeJpaRepository;

    private Integer conquistadoresId;
    private Integer belenId;
    private LocalDate bookingDate;
    private LocalTime bookingTime;

    @BeforeEach
    void setUp() {
        allocationJpaRepository.deleteAll();
        appointmentJpaRepository.deleteAll();
        scheduleJpaRepository.deleteAll();

        conquistadoresId = FacilityMasterData.SEDE_ID_CONQUISTADORES;
        belenId = FacilityMasterData.SEDE_ID_BELEN;
        bookingDate = nextOpenWeekday(LocalDate.now().plusDays(4));
        bookingTime = LocalTime.of(10, 0);
    }

    @Test
    void rejectsFifthSimultaneousPresencialAtConquistadores() {
        List<UUID> scheduleIds = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            String doctorId = UUID.randomUUID().toString();
            Schedule schedule = scheduleRepository.save(Schedule.builder()
                    .doctorId(doctorId)
                    .sedeId(conquistadoresId)
                    .specialty("Medicina general")
                    .dayOfWeek(bookingDate.getDayOfWeek())
                    .startTime(LocalTime.of(8, 0))
                    .endTime(LocalTime.of(18, 0))
                    .slotDurationMinutes(30)
                    .maxPatientsPerSlot(1)
                    .isActive(true)
                    .build());
            scheduleIds.add(schedule.getId());

            appointmentBookingService.bookAppointment(new AppointmentBookingRequest(
                    UUID.randomUUID(),
                    doctorId,
                    null,
                    schedule.getId(),
                    conquistadoresId,
                    bookingDate,
                    bookingTime,
                    AppointmentType.PRESENCIAL,
                    "Consulta " + i,
                    null,
                    null
            ));
        }

        String fifthDoctor = UUID.randomUUID().toString();
        Schedule fifthSchedule = scheduleRepository.save(Schedule.builder()
                .doctorId(fifthDoctor)
                .sedeId(conquistadoresId)
                .specialty("Medicina general")
                .dayOfWeek(bookingDate.getDayOfWeek())
                .startTime(LocalTime.of(8, 0))
                .endTime(LocalTime.of(18, 0))
                .slotDurationMinutes(30)
                .maxPatientsPerSlot(1)
                .isActive(true)
                .build());

        assertThrows(ResourceCapacityExceededException.class, () -> appointmentBookingService.bookAppointment(
                new AppointmentBookingRequest(
                        UUID.randomUUID(),
                        fifthDoctor,
                        null,
                        fifthSchedule.getId(),
                        conquistadoresId,
                        bookingDate,
                        bookingTime,
                        AppointmentType.PRESENCIAL,
                        "Quinto consultorio",
                        null,
                        null
                )
        ));
    }

    @Test
    void rejectsThirdSimultaneousPhysiotherapySessionAtConquistadores() {
        Schedule scheduleA = saveTherapySchedule(conquistadoresId, "fisio-a");
        Schedule scheduleB = saveTherapySchedule(conquistadoresId, "fisio-b");

        bookTherapy(scheduleA);
        bookTherapy(scheduleB);

        Schedule scheduleC = saveTherapySchedule(conquistadoresId, "fisio-c");
        assertThrows(ResourceCapacityExceededException.class, () -> bookTherapy(scheduleC));
    }

    @Test
    void allowsTherapyGroupInSingleRoomAtConquistadores() {
        Schedule groupSchedule = saveTherapySchedule(conquistadoresId, "fisio-grupo");
        for (int i = 0; i < 4; i++) {
            bookTherapy(groupSchedule);
        }
    }

    @Test
    void rejectsSecondSimultaneousPresencialAtBelen() {
        String doctorA = UUID.randomUUID().toString();
        String doctorB = UUID.randomUUID().toString();

        Schedule scheduleA = scheduleRepository.save(Schedule.builder()
                .doctorId(doctorA)
                .sedeId(belenId)
                .specialty("Psicologia")
                .dayOfWeek(bookingDate.getDayOfWeek())
                .startTime(LocalTime.of(8, 0))
                .endTime(LocalTime.of(18, 0))
                .slotDurationMinutes(30)
                .maxPatientsPerSlot(1)
                .isActive(true)
                .build());

        Schedule scheduleB = scheduleRepository.save(Schedule.builder()
                .doctorId(doctorB)
                .sedeId(belenId)
                .specialty("Medicina laboral")
                .dayOfWeek(bookingDate.getDayOfWeek())
                .startTime(LocalTime.of(8, 0))
                .endTime(LocalTime.of(18, 0))
                .slotDurationMinutes(30)
                .maxPatientsPerSlot(1)
                .isActive(true)
                .build());

        appointmentBookingService.bookAppointment(new AppointmentBookingRequest(
                UUID.randomUUID(),
                doctorA,
                null,
                scheduleA.getId(),
                belenId,
                bookingDate,
                bookingTime,
                AppointmentType.PRESENCIAL,
                "Belen 1",
                null,
                null
        ));

        assertThrows(ResourceCapacityExceededException.class, () -> appointmentBookingService.bookAppointment(
                new AppointmentBookingRequest(
                        UUID.randomUUID(),
                        doctorB,
                        null,
                        scheduleB.getId(),
                        belenId,
                        bookingDate,
                        bookingTime,
                        AppointmentType.PRESENCIAL,
                        "Belen 2",
                        null,
                        null
                )
        ));
    }

    private Schedule saveTherapySchedule(Integer sedeId, String doctorId) {
        return scheduleRepository.save(Schedule.builder()
                .doctorId(doctorId)
                .sedeId(sedeId)
                .specialty("Terapia fisica")
                .dayOfWeek(bookingDate.getDayOfWeek())
                .startTime(LocalTime.of(8, 0))
                .endTime(LocalTime.of(18, 0))
                .slotDurationMinutes(30)
                .maxPatientsPerSlot(6)
                .isActive(true)
                .build());
    }

    private static LocalDate nextOpenWeekday(LocalDate date) {
        while (date.getDayOfWeek() == DayOfWeek.SUNDAY) {
            date = date.plusDays(1);
        }
        return date;
    }

    private void bookTherapy(Schedule schedule) {
        appointmentBookingService.bookAppointment(new AppointmentBookingRequest(
                UUID.randomUUID(),
                schedule.getDoctorId(),
                null,
                schedule.getId(),
                schedule.getSedeId(),
                bookingDate,
                bookingTime,
                AppointmentType.TERAPIA_FISICA,
                "Sesion fisio",
                null,
                null
        ));
    }
}
