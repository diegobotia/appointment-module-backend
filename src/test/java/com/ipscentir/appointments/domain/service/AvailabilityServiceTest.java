package com.ipscentir.appointments.domain.service;

import com.ipscentir.appointments.domain.model.appointment.Appointment;
import com.ipscentir.appointments.domain.model.catalog.AppointmentServiceType;
import com.ipscentir.appointments.domain.model.schedule.AvailableSlotDetail;
import com.ipscentir.appointments.domain.model.schedule.AvailableSlot;
import com.ipscentir.appointments.domain.model.schedule.Schedule;
import com.ipscentir.appointments.domain.repository.AppointmentRepository;
import com.ipscentir.appointments.domain.repository.ScheduleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AvailabilityServiceTest {

    @Mock
    private ScheduleRepository scheduleRepository;

    @Mock
    private AppointmentRepository appointmentRepository;

    @InjectMocks
    private AvailabilityService availabilityService;

    private Schedule schedule;
    private UUID doctorId;
        private UUID facilityId;

    @BeforeEach
    void setUp() {
        doctorId = UUID.randomUUID();
        facilityId = UUID.randomUUID();
        schedule = Schedule.builder()
                .id(UUID.randomUUID())
                .doctorId(doctorId)
                .facilityId(facilityId)
                .dayOfWeek(DayOfWeek.MONDAY)
                .startTime(LocalTime.of(8, 0))
                .endTime(LocalTime.of(10, 0))
                .slotDurationMinutes(30)
                .maxPatientsPerSlot(1)
                .isActive(true)
                .build();
    }

    @Test
    void testGetAvailableSlots_WhenNoAppointments_ReturnsAllSlots() {
        LocalDate date = LocalDate.of(2023, 10, 2); // Lunes
        
        when(scheduleRepository.findByDoctorIdAndFacilityIdAndDayOfWeek(doctorId, facilityId, DayOfWeek.MONDAY))
                .thenReturn(Optional.of(schedule));
        when(appointmentRepository.findByDoctorIdAndDate(doctorId, date))
                .thenReturn(List.of());

        List<AvailableSlot> slots = availabilityService.getAvailableSlots(doctorId, facilityId, date);

        assertEquals(4, slots.size()); // 8:00, 8:30, 9:00, 9:30
    }

    @Test
    void testGetAvailableSlots_WhenAppointmentsExist_FiltersOccupiedSlots() {
        LocalDate date = LocalDate.of(2023, 10, 2); 
        
        Appointment appointment = org.mockito.Mockito.mock(Appointment.class);
        when(appointment.getAppointmentTime()).thenReturn(LocalTime.of(8, 30));

        when(scheduleRepository.findByDoctorIdAndFacilityIdAndDayOfWeek(doctorId, facilityId, DayOfWeek.MONDAY))
                .thenReturn(Optional.of(schedule));
        
        when(appointmentRepository.findByDoctorIdAndDate(doctorId, date))
                .thenReturn(List.of(appointment));

        List<AvailableSlot> slots = availabilityService.getAvailableSlots(doctorId, facilityId, date);

        assertEquals(3, slots.size());
        assertFalse(slots.stream().anyMatch(s -> s.getTime().equals(LocalTime.of(8, 30))));
    }

    @Test
    void testIsSlotAvailable_ReturnsTrueWhenFree() {
        LocalDate date = LocalDate.of(2023, 10, 2); 
        when(scheduleRepository.findByDoctorIdAndFacilityIdAndDayOfWeek(doctorId, facilityId, DayOfWeek.MONDAY))
                .thenReturn(Optional.of(schedule));
        when(appointmentRepository.findByDoctorIdAndDate(doctorId, date))
                .thenReturn(List.of());

        assertTrue(availabilityService.isSlotAvailable(doctorId, facilityId, date, LocalTime.of(9, 0)));
    }

    @Test
    void testGetNearestAvailableSlotsByServiceType_ReturnsClosestSlotsForFacility() {
        LocalDate fromDate = LocalDate.of(2023, 10, 2);
        Schedule serviceSchedule = Schedule.builder()
                .id(UUID.randomUUID())
                .doctorId(UUID.randomUUID())
                .facilityId(facilityId)
                .specialty("Terapia fisica")
                .dayOfWeek(DayOfWeek.MONDAY)
                .startTime(LocalTime.of(8, 0))
                .endTime(LocalTime.of(9, 0))
                .slotDurationMinutes(30)
                .maxPatientsPerSlot(2)
                .isActive(true)
                .build();

        when(scheduleRepository.findByFacilityIdAndDayOfWeek(facilityId, DayOfWeek.MONDAY))
                .thenReturn(List.of(serviceSchedule));
        when(appointmentRepository.findByDoctorIdAndDate(serviceSchedule.getDoctorId(), fromDate))
                .thenReturn(List.of());

        List<AvailableSlotDetail> slots = availabilityService.getNearestAvailableSlotsByServiceType(
                AppointmentServiceType.TERAPIA_FISICA,
                facilityId,
                fromDate,
                4
        );

                assertEquals(4, slots.size());
        assertEquals(AppointmentServiceType.TERAPIA_FISICA, slots.get(0).serviceType());
        assertEquals(facilityId, slots.get(0).facilityId());
                assertEquals(fromDate, slots.get(0).appointmentDate());
    }
}
