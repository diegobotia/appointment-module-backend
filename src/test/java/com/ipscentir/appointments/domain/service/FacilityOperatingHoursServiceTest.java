package com.ipscentir.appointments.domain.service;

import com.ipscentir.appointments.domain.model.facility.FacilityMasterData;

import com.ipscentir.appointments.application.exception.FacilityOperatingHoursViolationException;
import com.ipscentir.appointments.domain.model.sede.Sede;
import com.ipscentir.appointments.domain.model.facility.FacilityOperatingHour;
import com.ipscentir.appointments.domain.repository.FacilityOperatingHoursRepository;
import com.ipscentir.appointments.domain.repository.SedeRepository;
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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FacilityOperatingHoursServiceTest {

    @Mock
    private FacilityOperatingHoursRepository facilityOperatingHoursRepository;

    @Mock
    private SedeRepository sedeRepository;

    @InjectMocks
    private FacilityOperatingHoursService facilityOperatingHoursService;

    private final Integer sedeId = FacilityMasterData.SEDE_ID_BELEN;

    @Test
    void rejectsSlotBeforeOpeningOnWeekday() {
        stubBelenFacility();
        stubWeekdayHours();

        FacilityOperatingHoursViolationException ex = assertThrows(
                FacilityOperatingHoursViolationException.class,
                () -> facilityOperatingHoursService.assertSlotWithinSedeHours(
                        sedeId,
                        DayOfWeek.MONDAY,
                        LocalTime.of(6, 0),
                        LocalTime.of(10, 0)
                )
        );

        assertEquals(7, ex.allowedWindow().openTime().getHour());
        assertEquals(18, ex.allowedWindow().closeTime().getHour());
    }

    @Test
    void rejectsSaturdayAfternoonSlot() {
        stubBelenFacility();
        when(facilityOperatingHoursRepository.findBySedeIdOrderByDayOfWeek(sedeId))
                .thenReturn(List.of(
                        operatingHour(6, LocalTime.of(8, 0), LocalTime.of(12, 0), false)
                ));

        assertThrows(
                FacilityOperatingHoursViolationException.class,
                () -> facilityOperatingHoursService.assertSlotWithinSedeHours(
                        sedeId,
                        DayOfWeek.SATURDAY,
                        LocalTime.of(13, 0),
                        LocalTime.of(17, 0)
                )
        );
    }

    @Test
    void acceptsValidWeekdaySlot() {
        stubBelenFacility();
        stubWeekdayHours();

        assertDoesNotThrow(() -> facilityOperatingHoursService.assertSlotWithinSedeHours(
                sedeId,
                DayOfWeek.TUESDAY,
                LocalTime.of(9, 0),
                LocalTime.of(17, 0)
        ));
    }

    @Test
    void rejectsSundaySlot() {
        stubBelenFacility();
        when(facilityOperatingHoursRepository.findBySedeIdOrderByDayOfWeek(sedeId))
                .thenReturn(List.of(operatingHour(7, null, null, true)));

        FacilityOperatingHoursViolationException ex = assertThrows(
                FacilityOperatingHoursViolationException.class,
                () -> facilityOperatingHoursService.assertSlotWithinSedeHours(
                        sedeId,
                        DayOfWeek.SUNDAY,
                        LocalTime.of(9, 0),
                        LocalTime.of(11, 0)
                )
        );

        assertEquals(true, ex.allowedWindow().closed());
    }

    @Test
    void rejectsInstitutionalSlotBeforeOpening() {
        assertThrows(
                FacilityOperatingHoursViolationException.class,
                () -> facilityOperatingHoursService.assertSlotWithinInstitutionalHours(
                        DayOfWeek.MONDAY,
                        LocalTime.of(6, 0),
                        LocalTime.of(8, 0)
                )
        );
    }

    @Test
    void rejectsBlockSpanningSaturdayAfternoonForFacility() {
        stubBelenFacility();
        when(facilityOperatingHoursRepository.findBySedeIdOrderByDayOfWeek(sedeId))
                .thenReturn(List.of(
                        operatingHour(6, LocalTime.of(8, 0), LocalTime.of(12, 0), false)
                ));

        assertThrows(
                FacilityOperatingHoursViolationException.class,
                () -> facilityOperatingHoursService.assertBlockWithinFacilityHours(
                        sedeId,
                        LocalDate.of(2026, 5, 23),
                        LocalDate.of(2026, 5, 23),
                        LocalTime.of(13, 0),
                        LocalTime.of(15, 0)
                )
        );
    }

    private void stubBelenFacility() {
        when(sedeRepository.findById(sedeId)).thenReturn(Optional.of(
                Sede.builder()
                        .id(sedeId)
                        
                        .nombre("Sede Belén")
                        
                        
                        .build()
        ));
    }

    private void stubWeekdayHours() {
        when(facilityOperatingHoursRepository.findBySedeIdOrderByDayOfWeek(sedeId))
                .thenReturn(List.of(
                        operatingHour(2, LocalTime.of(7, 0), LocalTime.of(18, 0), false)
                ));
    }

    private FacilityOperatingHour operatingHour(int day, LocalTime open, LocalTime close, boolean closed) {
        return FacilityOperatingHour.builder()
                .id(UUID.randomUUID())
                .sedeId(sedeId)
                .dayOfWeek(day)
                .openTime(open)
                .closeTime(close)
                .closed(closed)
                .build();
    }
}
