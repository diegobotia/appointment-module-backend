package com.ipscentir.appointments.domain.service;

import com.ipscentir.appointments.domain.model.facility.FacilityMasterData;

import com.ipscentir.appointments.domain.model.appointment.Appointment;
import com.ipscentir.appointments.domain.model.appointment.AppointmentScheduleData;
import com.ipscentir.appointments.domain.model.appointment.AppointmentStatus;
import com.ipscentir.appointments.domain.model.appointment.AppointmentType;
import com.ipscentir.appointments.domain.model.schedule.Schedule;
import com.ipscentir.appointments.domain.repository.AppointmentRepository;
import com.ipscentir.appointments.domain.repository.ScheduleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class HumanResourceAvailabilityServiceTest {

        @Mock
        private AvailabilityService availabilityService;

        @Mock
        private FacilityOperatingHoursService facilityOperatingHoursService;

        @Mock
        private ScheduleRepository scheduleRepository;

        @Mock
        private AppointmentRepository appointmentRepository;

        @InjectMocks
        private HumanResourceAvailabilityService humanResourceAvailabilityService;

        private UUID patientId;
        private String doctorId;
        private String secondaryDoctorId;
        private Integer sedeId;
        private UUID scheduleId;
        private LocalDate date;
        private LocalTime time;

        @BeforeEach
        void setUp() {
                patientId = UUID.randomUUID();
                doctorId = UUID.randomUUID().toString();
                secondaryDoctorId = UUID.randomUUID().toString();
                sedeId = FacilityMasterData.SEDE_ID_BELEN;
                scheduleId = UUID.randomUUID();
                date = LocalDate.now().plusDays(3);
                time = LocalTime.of(10, 0);

                Schedule schedule = Schedule.builder()
                                .id(scheduleId)
                                .doctorId(doctorId)
                                .sedeId(sedeId)
                                .dayOfWeek(date.getDayOfWeek())
                                .startTime(LocalTime.of(7, 0))
                                .endTime(LocalTime.of(18, 0))
                                .slotDurationMinutes(30)
                                .maxPatientsPerSlot(6)
                                .specialty("MEDICO_FISIATRIA")
                                .isActive(true)
                                .build();

                when(scheduleRepository.findById(scheduleId)).thenReturn(Optional.of(schedule));
                doNothing().when(facilityOperatingHoursService).assertSlotWithinSedeHours(
                                any(), any(), any(), any());
        }

        @Test
        void assertBookingAllowedSucceedsWhenSlotAndPatientAreFree() {
                when(availabilityService.isDoctorSlotAvailable(doctorId, sedeId, date, time, null)).thenReturn(true);
                when(appointmentRepository.existsByPatientIdAndDate(patientId, date)).thenReturn(false);

                HumanResourceBookingContext context = HumanResourceBookingContext.forBooking(
                                patientId, doctorId, null, scheduleId, sedeId, date, time,
                                AppointmentType.PRESENCIAL, 30);

                assertDoesNotThrow(() -> humanResourceAvailabilityService.assertBookingAllowed(context));
        }

        @Test
        void assertBookingAllowedFailsWhenDoctorSlotUnavailable() {
                when(availabilityService.isDoctorSlotAvailable(doctorId, sedeId, date, time)).thenReturn(false);

                HumanResourceBookingContext context = HumanResourceBookingContext.forBooking(
                                patientId, doctorId, null, scheduleId, sedeId, date, time,
                                AppointmentType.PRESENCIAL, 30);

                assertThrows(
                                IllegalStateException.class,
                                () -> humanResourceAvailabilityService.assertBookingAllowed(context));
        }

        @Test
        void assertRescheduleAllowedExcludesCurrentAppointmentFromDuplicateCheck() {
                UUID appointmentId = UUID.randomUUID();
                when(availabilityService.isDoctorSlotAvailable(doctorId, sedeId, date, time, appointmentId))
                                .thenReturn(true);
                when(appointmentRepository.existsByPatientIdAndDateExcluding(patientId, date, appointmentId))
                                .thenReturn(false);

                HumanResourceBookingContext context = HumanResourceBookingContext.forBooking(
                                patientId, doctorId, null, scheduleId, sedeId, date, time,
                                AppointmentType.PRESENCIAL, 30);

                assertDoesNotThrow(
                                () -> humanResourceAvailabilityService.assertRescheduleAllowed(context, appointmentId));
                verify(appointmentRepository).existsByPatientIdAndDateExcluding(patientId, date, appointmentId);
                verify(availabilityService).isDoctorSlotAvailable(doctorId, sedeId, date, time, appointmentId);
        }

        @Test
        void juntaMedicaRequiresSecondarySpecialist() {
                when(availabilityService.isDoctorSlotAvailable(doctorId, sedeId, date, time)).thenReturn(true);

                HumanResourceBookingContext context = HumanResourceBookingContext.forBooking(
                                patientId, doctorId, null, scheduleId, sedeId, date, time,
                                AppointmentType.JUNTA_MEDICA, 30);

                assertThrows(
                                IllegalStateException.class,
                                () -> humanResourceAvailabilityService.assertBookingAllowed(context));
        }

        @Test
        void therapyGroupRejectsWhenSlotAtMaxCapacity() {
                UUID scheduleForTherapy = scheduleId;
                Schedule therapySchedule = Schedule.builder()
                                .id(scheduleForTherapy)
                                .doctorId(doctorId)
                                .sedeId(sedeId)
                                .dayOfWeek(date.getDayOfWeek())
                                .startTime(LocalTime.of(7, 0))
                                .endTime(LocalTime.of(18, 0))
                                .slotDurationMinutes(30)
                                .maxPatientsPerSlot(6)
                                .specialty("TERAPIA_FISICA")
                                .isActive(true)
                                .build();
                when(scheduleRepository.findById(scheduleForTherapy)).thenReturn(Optional.of(therapySchedule));

                List<Appointment> sixActive = java.util.stream.IntStream.range(0, 6)
                                .mapToObj(i -> therapyAppointment(scheduleForTherapy))
                                .toList();
                when(appointmentRepository.findByScheduleAndDateAndTimeAndTypeForUpdate(
                                scheduleForTherapy, date, time, AppointmentType.TERAPIA_FISICA)).thenReturn(sixActive);

                assertThrows(
                                IllegalStateException.class,
                                () -> humanResourceAvailabilityService.assertTherapyGroupAllowsNewPatient(
                                                scheduleForTherapy, date, time, AppointmentType.TERAPIA_FISICA));
        }

        @Test
        void staffMeetingStubValidatesAllParticipants() {
                String participantA = UUID.randomUUID().toString();
                String participantB = UUID.randomUUID().toString();
                when(availabilityService.isDoctorSlotAvailable(participantA, sedeId, date, time)).thenReturn(true);
                when(availabilityService.isDoctorSlotAvailable(participantB, sedeId, date, time)).thenReturn(false);

                assertThrows(
                                IllegalStateException.class,
                                () -> humanResourceAvailabilityService.assertStaffMeetingParticipantsAvailable(
                                                List.of(participantA, participantB),
                                                sedeId,
                                                date,
                                                time,
                                                60));
        }

        private Appointment therapyAppointment(UUID scheduleForTherapy) {
                return Appointment.scheduleNew(
                                UUID.randomUUID(),
                                doctorId,
                                null,
                                new AppointmentScheduleData(
                                                scheduleForTherapy,
                                                sedeId,
                                                date,
                                                time,
                                                30,
                                                AppointmentType.TERAPIA_FISICA,
                                                AppointmentStatus.SCHEDULED,
                                                "Terapia"));
        }
}
