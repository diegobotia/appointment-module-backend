package com.ipscentir.appointments.domain.model.schedule;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScheduleTest {

    private Schedule schedule;

    @BeforeEach
    void setUp() {
        schedule = Schedule.builder()
                .id(UUID.randomUUID())
                .doctorId(UUID.randomUUID().toString())
                .facilityId(UUID.randomUUID())
                .dayOfWeek(DayOfWeek.MONDAY)
                .startTime(LocalTime.of(8, 0))
                .endTime(LocalTime.of(12, 0))
                .slotDurationMinutes(30)
                .maxPatientsPerSlot(1)
                .isActive(true)
                .build();
    }

    @Test
    void testGetAvailableSlots_ReturnsCorrectNumberOfSlots() {
        // Monday date
        LocalDate date = LocalDate.of(2023, 10, 2); // 2023-10-02 was a Monday
        
        List<LocalTime> slots = schedule.getAvailableSlots(date);
        
        // From 8:00 to 12:00 with 30 min slots = 8 slots
        assertEquals(8, slots.size());
        assertEquals(LocalTime.of(8, 0), slots.get(0));
        assertEquals(LocalTime.of(11, 30), slots.get(7));
    }

    @Test
    void testGetAvailableSlots_WrongDayOfWeek_ReturnsEmpty() {
        // Tuesday
        LocalDate date = LocalDate.of(2023, 10, 3);
        List<LocalTime> slots = schedule.getAvailableSlots(date);
        assertTrue(slots.isEmpty());
    }

    @Test
    void testGetAvailableSlots_WithBlock_ReturnsFilteredSlots() {
        LocalDate date = LocalDate.of(2023, 10, 2);
        
        // Bloquear 9:00 a 10:00
        schedule.addBlock(date, LocalTime.of(9, 0), LocalTime.of(10, 0), "Meeting");
        
        List<LocalTime> slots = schedule.getAvailableSlots(date);
        
        // Removes 9:00 and 9:30. Total 8 - 2 = 6 slots
        assertEquals(6, slots.size());
        assertFalse(slots.contains(LocalTime.of(9, 0)));
        assertFalse(slots.contains(LocalTime.of(9, 30)));
        // 10:00 starts anew outside the block because block check is exclusive of the end
        assertTrue(slots.contains(LocalTime.of(10, 0)));
    }

    @Test
    void testIsAvailable_Success() {
        LocalDate date = LocalDate.of(2023, 10, 2);
        assertTrue(schedule.isAvailable(date, LocalTime.of(9, 30)));
    }

    @Test
    void testIsAvailable_Fails_WhenOutsideHours() {
        LocalDate date = LocalDate.of(2023, 10, 2);
        assertFalse(schedule.isAvailable(date, LocalTime.of(7, 30)));
        assertFalse(schedule.isAvailable(date, LocalTime.of(12, 30)));
    }
}
