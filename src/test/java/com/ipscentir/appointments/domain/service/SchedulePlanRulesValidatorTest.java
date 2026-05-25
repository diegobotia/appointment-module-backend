package com.ipscentir.appointments.domain.service;

import com.ipscentir.appointments.application.dto.schedule.CreateSchedulePlanSlotRequest;
import com.ipscentir.appointments.domain.model.facility.FacilityResource;
import com.ipscentir.appointments.domain.model.facility.FacilityResourceType;
import com.ipscentir.appointments.domain.model.schedule.SchedulePlan;
import com.ipscentir.appointments.domain.model.schedule.SchedulePlanSlot;
import com.ipscentir.appointments.infrastructure.persistence.jpa.FacilityResourceJpaRepository;
import com.ipscentir.appointments.infrastructure.persistence.jpa.SchedulePlanJpaRepository;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SchedulePlanRulesValidatorTest {

    @Mock
    private SchedulePlanJpaRepository schedulePlanJpaRepository;

    @Mock
    private FacilityResourceJpaRepository facilityResourceJpaRepository;

    @InjectMocks
    private SchedulePlanRulesValidator validator;

    @Test
    void acceptsDurationBetweenTwoAndThreeMonths() {
        assertDoesNotThrow(() -> validator.validateDuration(
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 6, 30)
        ));
    }

    @Test
    void rejectsDurationShorterThanTwoMonths() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                validator.validateDuration(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 5, 31))
        );
        assertTrue(ex.getMessage().contains("at least 2 months"));
    }

    @Test
    void rejectsDurationLongerThanThreeMonths() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                validator.validateDuration(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 8, 1))
        );
        assertTrue(ex.getMessage().contains("not exceed 3 months"));
    }

    @Test
    void rejectsDailyWorkloadBelowEightHours() {
        List<CreateSchedulePlanSlotRequest> slots = List.of(
                new CreateSchedulePlanSlotRequest(
                        DayOfWeek.MONDAY,
                        LocalTime.of(8, 0),
                        LocalTime.of(12, 0),
                        30,
                        1,
                        UUID.randomUUID()
                )
        );

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                validator.validateSlotsForCreation(slots)
        );
        assertTrue(ex.getMessage().contains("minimum workload"));
    }

    @Test
    void acceptsSaturdayHalfDayWorkload() {
        List<CreateSchedulePlanSlotRequest> slots = List.of(
                new CreateSchedulePlanSlotRequest(
                        DayOfWeek.SATURDAY,
                        LocalTime.of(8, 0),
                        LocalTime.of(12, 0),
                        30,
                        1,
                        UUID.randomUUID()
                )
        );

        assertDoesNotThrow(() -> validator.validateSlotsForCreation(slots));
    }

    @Test
    void allowsPublishReplacementForSamePeriod() {
        UUID publishingPlanId = UUID.randomUUID();
        SchedulePlan activePrevious = SchedulePlan.builder()
                .id(UUID.randomUUID())
                .specialistId("med-1")
                .sedeId(2)
                .startDate(LocalDate.of(2026, 4, 1))
                .endDate(LocalDate.of(2026, 6, 30))
                .versionNumber(1)
                .published(true)
                .activeVersion(true)
                .build();

        SchedulePlan publishing = SchedulePlan.builder()
                .id(publishingPlanId)
                .specialistId("med-1")
                .sedeId(2)
                .startDate(LocalDate.of(2026, 4, 1))
                .endDate(LocalDate.of(2026, 6, 30))
                .versionNumber(2)
                .published(false)
                .activeVersion(false)
                .slots(List.of(activeSlot()))
                .build();

        when(schedulePlanJpaRepository.findOverlappingActivePlans(any(), any(), any()))
                .thenReturn(List.of(activePrevious));

        assertDoesNotThrow(() -> validator.validateNoActiveDoctorOverlapOnPublish(publishing));
    }

    @Test
    void rejectsPublishWhenOverlappingDifferentPeriod() {
        SchedulePlan activePrevious = SchedulePlan.builder()
                .id(UUID.randomUUID())
                .specialistId("med-1")
                .sedeId(2)
                .startDate(LocalDate.of(2026, 4, 1))
                .endDate(LocalDate.of(2026, 6, 30))
                .versionNumber(1)
                .published(true)
                .activeVersion(true)
                .build();

        SchedulePlan publishing = SchedulePlan.builder()
                .id(UUID.randomUUID())
                .specialistId("med-1")
                .sedeId(2)
                .startDate(LocalDate.of(2026, 5, 1))
                .endDate(LocalDate.of(2026, 7, 31))
                .versionNumber(1)
                .published(false)
                .activeVersion(false)
                .slots(List.of(activeSlot()))
                .build();

        when(schedulePlanJpaRepository.findOverlappingActivePlans(any(), any(), any()))
                .thenReturn(List.of(activePrevious));

        assertThrows(IllegalArgumentException.class, () ->
                validator.validateNoActiveDoctorOverlapOnPublish(publishing)
        );
    }

    @Test
    void rejectsConsultorioFromAnotherSede() {
        UUID consultorioId = UUID.randomUUID();
        when(facilityResourceJpaRepository.findById(consultorioId)).thenReturn(Optional.of(
                FacilityResource.builder()
                        .id(consultorioId)
                        .sedeId(1)
                        .resourceType(FacilityResourceType.CONSULTORIO)
                        .code("OTHER")
                        .displayName("Otro")
                        .active(true)
                        .build()
        ));

        List<CreateSchedulePlanSlotRequest> slots = List.of(
                new CreateSchedulePlanSlotRequest(
                        DayOfWeek.MONDAY,
                        LocalTime.of(8, 0),
                        LocalTime.of(12, 0),
                        30,
                        1,
                        consultorioId
                ),
                new CreateSchedulePlanSlotRequest(
                        DayOfWeek.MONDAY,
                        LocalTime.of(13, 0),
                        LocalTime.of(17, 0),
                        30,
                        1,
                        consultorioId
                )
        );

        assertThrows(IllegalArgumentException.class, () ->
                validator.validateConsultoriosBelongToSede(2, slots)
        );
    }

    private SchedulePlanSlot activeSlot() {
        return SchedulePlanSlot.builder()
                .dayOfWeek(DayOfWeek.MONDAY)
                .startTime(LocalTime.of(8, 0))
                .endTime(LocalTime.of(12, 0))
                .slotDurationMinutes(30)
                .maxPatientsPerSlot(1)
                .consultorioId(UUID.randomUUID())
                .active(true)
                .build();
    }
}
