package com.ipscentir.appointments.domain.service;

import com.ipscentir.appointments.domain.model.facility.FacilityMasterData;

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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AvailabilityServiceTest {

    @Mock
    private ScheduleRepository scheduleRepository;

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private ResourceCapacityService resourceCapacityService;

    @InjectMocks
    private AvailabilityService availabilityService;

    private Schedule schedule;
    private String doctorId;
        private Integer sedeId;

    @BeforeEach
    void setUp() {
        doctorId = java.util.UUID.randomUUID().toString();
        sedeId = FacilityMasterData.SEDE_ID_BELEN;
        schedule = Schedule.builder()
                .id(UUID.randomUUID())
                                .doctorId(doctorId.toString())
                .sedeId(sedeId)
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
        
        when(scheduleRepository.findByDoctorIdAndSedeIdAndDayOfWeek(doctorId.toString(), sedeId, DayOfWeek.MONDAY))
                .thenReturn(Optional.of(schedule));
        when(appointmentRepository.findByDoctorIdAndDate(doctorId.toString(), date))
                .thenReturn(List.of());

        List<AvailableSlot> slots = availabilityService.getAvailableSlots(doctorId.toString(), sedeId, date);

        assertEquals(4, slots.size()); // 8:00, 8:30, 9:00, 9:30
    }

    @Test
    void testGetAvailableSlots_WhenAppointmentsExist_FiltersOccupiedSlots() {
        LocalDate date = LocalDate.of(2023, 10, 2); 
        
        Appointment appointment = org.mockito.Mockito.mock(Appointment.class);
        when(appointment.getAppointmentTime()).thenReturn(LocalTime.of(8, 30));

        when(scheduleRepository.findByDoctorIdAndSedeIdAndDayOfWeek(doctorId.toString(), sedeId, DayOfWeek.MONDAY))
                .thenReturn(Optional.of(schedule));
        
        when(appointmentRepository.findByDoctorIdAndDate(doctorId.toString(), date))
                .thenReturn(List.of(appointment));

        List<AvailableSlot> slots = availabilityService.getAvailableSlots(doctorId.toString(), sedeId, date);

        assertEquals(3, slots.size());
        assertFalse(slots.stream().anyMatch(s -> s.getTime().equals(LocalTime.of(8, 30))));
    }

    @Test
    void testIsSlotAvailable_ReturnsTrueWhenFree() {
        LocalDate date = LocalDate.of(2023, 10, 2); 
        when(scheduleRepository.findByDoctorIdAndSedeIdAndDayOfWeek(doctorId.toString(), sedeId, DayOfWeek.MONDAY))
                .thenReturn(Optional.of(schedule));
        when(appointmentRepository.findByDoctorIdAndDate(doctorId.toString(), date))
                .thenReturn(List.of());

        assertTrue(availabilityService.isSlotAvailable(doctorId.toString(), sedeId, date, LocalTime.of(9, 0)));
    }

    @Test
    void testGetNearestAvailableSlotsByServiceType_ReturnsClosestSlotsForFacility() {
        LocalDate fromDate = LocalDate.of(2023, 10, 2);
        Schedule serviceSchedule = Schedule.builder()
                .id(UUID.randomUUID())
                .doctorId(UUID.randomUUID().toString())
                .sedeId(sedeId)
                .specialty("Terapia fisica")
                .dayOfWeek(DayOfWeek.MONDAY)
                .startTime(LocalTime.of(8, 0))
                .endTime(LocalTime.of(9, 0))
                .slotDurationMinutes(30)
                .maxPatientsPerSlot(2)
                .isActive(true)
                .build();

        when(scheduleRepository.findBySedeIdAndDayOfWeek(sedeId, DayOfWeek.MONDAY))
                .thenReturn(List.of(serviceSchedule));
        when(appointmentRepository.findByDoctorIdAndDate(serviceSchedule.getDoctorId(), fromDate))
                .thenReturn(List.of());
        lenient().when(resourceCapacityService.hasPhysicalCapacityForService(
                any(), any(), any(), any(), any(), anyInt()
        )).thenReturn(true);

        List<AvailableSlotDetail> slots = availabilityService.getNearestAvailableSlotsByServiceType(
                AppointmentServiceType.TERAPIA_FISICA,
                sedeId,
                fromDate,
                4
        );

                assertEquals(4, slots.size());
        assertEquals(AppointmentServiceType.TERAPIA_FISICA, slots.get(0).serviceType());
        assertEquals(sedeId, slots.get(0).sedeId());
                assertEquals(fromDate, slots.get(0).appointmentDate());
    }

    @Test
    void getAvailableSlotsInRangeReturnsSlotsAcrossDays() {
        LocalDate from = LocalDate.of(2023, 10, 2);
        LocalDate to = LocalDate.of(2023, 10, 3);
        when(scheduleRepository.findByDoctorIdAndSedeIdAndDayOfWeek(doctorId, sedeId, DayOfWeek.MONDAY))
                .thenReturn(Optional.of(schedule));
        when(scheduleRepository.findByDoctorIdAndSedeIdAndDayOfWeek(doctorId, sedeId, DayOfWeek.TUESDAY))
                .thenReturn(Optional.empty());
        when(appointmentRepository.findByDoctorIdAndDate(doctorId, from)).thenReturn(List.of());

        List<AvailableSlotDetail> slots = availabilityService.getAvailableSlotsInRange(doctorId, sedeId, from, to);

        assertEquals(4, slots.size());
    }

    @Test
    void isSlotAvailableWithoutFacilityReturnsFalseWhenNoSchedule() {
        LocalDate date = LocalDate.of(2023, 10, 3);
        when(scheduleRepository.findByDoctorIdAndDayOfWeek(doctorId, date.getDayOfWeek()))
                .thenReturn(Optional.empty());

        assertFalse(availabilityService.isSlotAvailable(doctorId, date, LocalTime.of(9, 0)));
    }

    @Test
    void getNearestAvailableSlotsRejectsInvalidLimit() {
        assertThrows(IllegalArgumentException.class, () ->
                availabilityService.getNearestAvailableSlotsByServiceType(
                        AppointmentServiceType.TERAPIA_FISICA, sedeId, LocalDate.now(), 0));
    }
}
