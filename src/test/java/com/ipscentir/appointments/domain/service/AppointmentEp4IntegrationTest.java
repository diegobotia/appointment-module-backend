package com.ipscentir.appointments.domain.service;

import com.ipscentir.appointments.domain.model.appointment.Appointment;
import com.ipscentir.appointments.domain.model.appointment.AppointmentStatus;
import com.ipscentir.appointments.domain.model.appointment.AppointmentType;
import com.ipscentir.appointments.domain.model.facility.FacilityMasterData;
import com.ipscentir.appointments.domain.model.schedule.Schedule;
import com.ipscentir.appointments.domain.repository.ScheduleRepository;
import com.ipscentir.appointments.infrastructure.persistence.jpa.AppointmentJpaRepository;
import com.ipscentir.appointments.infrastructure.persistence.jpa.AppointmentResourceAllocationJpaRepository;
import com.ipscentir.appointments.infrastructure.persistence.jpa.SedeJpaRepository;
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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class AppointmentEp4IntegrationTest {

    @Autowired
    private AppointmentBookingService appointmentBookingService;

    @Autowired
    private ScheduleRepository scheduleRepository;

    @Autowired
    private AppointmentJpaRepository appointmentJpaRepository;

    @Autowired
    private AppointmentResourceAllocationJpaRepository allocationJpaRepository;

    @Autowired
    private SedeJpaRepository sedeJpaRepository;

    private String doctorA;
    private String doctorB;
    private Integer sedeId;
    private UUID scheduleIdA;
    private UUID scheduleIdJunta;
    private LocalDate bookingDate;
    private LocalTime bookingTime;

    @BeforeEach
    void setUp() {
        allocationJpaRepository.deleteAll();
        appointmentJpaRepository.deleteAll();

        doctorA = java.util.UUID.randomUUID().toString();
        doctorB = java.util.UUID.randomUUID().toString();
        sedeId = FacilityMasterData.SEDE_ID_BELEN;
        bookingDate = nextOpenWeekday(LocalDate.now().plusDays(2));
        bookingTime = LocalTime.of(10, 0);

        Schedule scheduleA = scheduleRepository.save(Schedule.builder()
            .doctorId(doctorA)
            .sedeId(sedeId)
                .specialty("TERAPIA_FISICA")
                .dayOfWeek(bookingDate.getDayOfWeek())
                .startTime(LocalTime.of(8, 0))
                .endTime(LocalTime.of(18, 0))
                .slotDurationMinutes(30)
                .maxPatientsPerSlot(20)
                .isActive(true)
                .build());

        Schedule scheduleJunta = scheduleRepository.save(Schedule.builder()
            .doctorId(doctorB)
            .sedeId(sedeId)
                .specialty("JUNTA_MEDICA")
                .dayOfWeek(bookingDate.getDayOfWeek())
                .startTime(LocalTime.of(8, 0))
                .endTime(LocalTime.of(18, 0))
                .slotDurationMinutes(30)
                .maxPatientsPerSlot(20)
                .isActive(true)
                .build());

        scheduleIdA = scheduleA.getId();
        scheduleIdJunta = scheduleJunta.getId();
    }

    @Test
    void shouldPromoteTherapyAppointmentsWhenGroupReachesFour() {
        for (int i = 0; i < 4; i++) {
            appointmentBookingService.bookAppointment(new AppointmentBookingRequest(
                UUID.randomUUID(),
                doctorA,
                null,
                scheduleIdA,
                sedeId,
                bookingDate,
                bookingTime,
                AppointmentType.TERAPIA_FISICA,
                "Terapia grupal",
                null,
                null
            ));
        }

        List<Appointment> appointments = appointmentJpaRepository
                .findByScheduleIdAndAppointmentDateAndAppointmentTimeAndAppointmentType(
                        scheduleIdA,
                        bookingDate,
                        bookingTime,
                        AppointmentType.TERAPIA_FISICA
                );

        assertThat(appointments)
            .hasSize(4)
            .allMatch(a -> a.getStatus() == AppointmentStatus.SCHEDULED);
    }

    @Test
    void shouldNotExceedTherapyCapacityUnderConcurrency() throws Exception {
        int attempts = 12;
        ExecutorService executorService = Executors.newFixedThreadPool(8);
        CountDownLatch startGate = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < attempts; i++) {
            futures.add(executorService.submit(() -> {
                try {
                    startGate.await();
                        appointmentBookingService.bookAppointment(new AppointmentBookingRequest(
                            UUID.randomUUID(),
                            doctorA,
                            null,
                            scheduleIdA,
                            sedeId,
                            bookingDate,
                            bookingTime,
                            AppointmentType.TERAPIA_OCUPACIONAL,
                            "Terapia ocupacional",
                            null,
                            null
                        ));
                } catch (Exception ignored) {
                    // Some attempts are expected to fail once capacity reaches 6.
                }
            }));
        }

        startGate.countDown();
        for (Future<?> future : futures) {
            future.get();
        }
        executorService.shutdown();

        List<Appointment> appointments = appointmentJpaRepository
                .findByScheduleIdAndAppointmentDateAndAppointmentTimeAndAppointmentType(
                        scheduleIdA,
                        bookingDate,
                        bookingTime,
                        AppointmentType.TERAPIA_OCUPACIONAL
                );

        long activeCount = appointments.stream()
                .filter(a -> a.getStatus() != AppointmentStatus.CANCELLED && a.getStatus() != AppointmentStatus.NO_SHOW)
                .count();

        assertThat(activeCount).isLessThanOrEqualTo(6);
    }

    @Test
    void shouldCreateJuntaMedicaWithTwoParticipants() {
        Appointment appointment = appointmentBookingService.bookAppointment(new AppointmentBookingRequest(
                UUID.randomUUID(),
                doctorB,
                doctorA,
                scheduleIdJunta,
            sedeId,
                bookingDate,
                bookingTime,
                AppointmentType.JUNTA_MEDICA,
                "Junta medica",
                null,
                null
        ));

            UUID appointmentId = java.util.Objects.requireNonNull(appointment.getId());
            Appointment reloaded = appointmentJpaRepository.findById(appointmentId).orElseThrow();
        assertThat(reloaded.getParticipants()).hasSize(2);
        assertThat(reloaded.getSecondaryDoctorId()).isEqualTo(doctorA);
    }

    private static LocalDate nextOpenWeekday(LocalDate date) {
        while (date.getDayOfWeek() == DayOfWeek.SUNDAY) {
            date = date.plusDays(1);
        }
        return date;
    }
}
