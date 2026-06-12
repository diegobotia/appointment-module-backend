package com.ipscentir.appointments.domain.service;

import com.ipscentir.appointments.application.dto.schedule.CreateSchedulePlanSlotRequest;
import com.ipscentir.appointments.domain.model.facility.FacilityResource;
import com.ipscentir.appointments.domain.model.schedule.SchedulePlan;
import com.ipscentir.appointments.domain.model.schedule.SchedulePlanSlot;
import com.ipscentir.appointments.infrastructure.persistence.jpa.FacilityResourceJpaRepository;
import com.ipscentir.appointments.infrastructure.persistence.jpa.SchedulePlanJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SchedulePlanRulesValidator {

    public static final int MIN_DURATION_MONTHS = 1;

    private final SchedulePlanJpaRepository schedulePlanJpaRepository;
    private final FacilityResourceJpaRepository facilityResourceJpaRepository;

    public void validateDuration(LocalDate startDate, LocalDate endDate) {
        if (!endDate.isAfter(startDate)) {
            throw new IllegalArgumentException("Schedule plan end date must be after start date");
        }

        long months = ChronoUnit.MONTHS.between(startDate, endDate);
        if (months < MIN_DURATION_MONTHS) {
            throw new IllegalArgumentException(
                    "Schedule plan duration must be at least " + MIN_DURATION_MONTHS + " month(s)");
        }
    }

    public void validateSlotsForCreation(List<CreateSchedulePlanSlotRequest> slots) {
        if (slots == null || slots.isEmpty()) {
            throw new IllegalArgumentException("Schedule plan must include at least one slot");
        }
        validateSlotTimeRangesFromRequests(slots);
        validateSameConsultorioPerDayFromRequests(slots);
        validateNoInternalSlotOverlapFromRequests(slots);
    }

    public void validateSlotsForPublication(SchedulePlan plan) {
        List<SchedulePlanSlot> activeSlots = plan.getSlots().stream()
                .filter(SchedulePlanSlot::isActive)
                .toList();
        if (activeSlots.isEmpty()) {
            throw new IllegalArgumentException("Cannot publish a schedule plan without active slots");
        }
        validateSlotTimeRangesFromEntities(activeSlots);
        validateSameConsultorioPerDayFromEntities(activeSlots);
        validateNoInternalSlotOverlapFromEntities(activeSlots);
        validateConsultoriosBelongToSedeForSlots(plan.getSedeId(), activeSlots);
    }

    public void validateConsultoriosBelongToSede(Integer sedeId, List<CreateSchedulePlanSlotRequest> slots) {
        Set<UUID> consultorioIds = new HashSet<>();
        for (CreateSchedulePlanSlotRequest slot : slots) {
            consultorioIds.add(slot.consultorioId());
        }
        validateConsultorioIdsForSede(sedeId, consultorioIds);
    }

    public void validateNoActiveDoctorOverlapOnPublish(SchedulePlan plan) {
        assertNoConflictingActiveDoctorPlans(
                plan.getSpecialistId(),
                plan.getStartDate(),
                plan.getEndDate(),
                plan.getId(),
                true
        );
    }

    public void validateNoConsultorioOverlapWithOtherDoctors(
            String specialistId,
            LocalDate startDate,
            LocalDate endDate,
            List<CreateSchedulePlanSlotRequest> slots
    ) {
        assertNoConsultorioOverlap(specialistId, startDate, endDate, slots, null);
    }

    public void validateNoConsultorioOverlapWithOtherDoctorsOnPublish(SchedulePlan plan) {
        List<CreateSchedulePlanSlotRequest> slotViews = plan.getSlots().stream()
                .filter(SchedulePlanSlot::isActive)
                .map(slot -> new CreateSchedulePlanSlotRequest(
                        slot.getDayOfWeek(),
                        slot.getStartTime(),
                        slot.getEndTime(),
                        slot.getSlotDurationMinutes(),
                        slot.getMaxPatientsPerSlot(),
                        slot.getConsultorioId()
                ))
                .toList();
        assertNoConsultorioOverlap(
                plan.getSpecialistId(),
                plan.getStartDate(),
                plan.getEndDate(),
                slotViews,
                plan.getId()
        );
    }

    private void assertNoConflictingActiveDoctorPlans(
            String specialistId,
            LocalDate startDate,
            LocalDate endDate,
            UUID excludePlanId,
            boolean allowSamePeriodReplacement
    ) {
        List<SchedulePlan> overlapping = schedulePlanJpaRepository.findOverlappingActivePlans(
                specialistId,
                startDate,
                endDate
        );

        for (SchedulePlan other : overlapping) {
            if (excludePlanId != null && excludePlanId.equals(other.getId())) {
                continue;
            }
            if (allowSamePeriodReplacement && samePeriod(other, startDate, endDate)) {
                continue;
            }
            throw new IllegalArgumentException(
                    "The schedule plan overlaps with an existing active plan for this doctor");
        }
    }

    private void assertNoConsultorioOverlap(
            String specialistId,
            LocalDate startDate,
            LocalDate endDate,
            List<CreateSchedulePlanSlotRequest> slots,
            UUID excludePlanId
    ) {
        List<SchedulePlan> otherPlans = schedulePlanJpaRepository.findAllOverlappingActivePlans(startDate, endDate);
        for (SchedulePlan otherPlan : otherPlans) {
            if (otherPlan.getSpecialistId().equals(specialistId)) {
                continue;
            }
            if (excludePlanId != null && excludePlanId.equals(otherPlan.getId())) {
                continue;
            }
            for (SchedulePlanSlot otherSlot : otherPlan.getSlots()) {
                if (!otherSlot.isActive()) {
                    continue;
                }
                for (CreateSchedulePlanSlotRequest newSlot : slots) {
                    if (newSlot.dayOfWeek() != otherSlot.getDayOfWeek()
                            || !newSlot.consultorioId().equals(otherSlot.getConsultorioId())) {
                        continue;
                    }
                    if (timesOverlap(
                            newSlot.startTime(),
                            newSlot.endTime(),
                            otherSlot.getStartTime(),
                            otherSlot.getEndTime()
                    )) {
                        throw new IllegalArgumentException(
                                "Consultorio is already occupied on " + newSlot.dayOfWeek()
                                        + " between " + otherSlot.getStartTime()
                                        + " and " + otherSlot.getEndTime()
                                        + " by another doctor");
                    }
                }
            }
        }
    }

    private void validateConsultoriosBelongToSedeForSlots(Integer sedeId, List<SchedulePlanSlot> slots) {
        Set<UUID> consultorioIds = new HashSet<>();
        for (SchedulePlanSlot slot : slots) {
            consultorioIds.add(slot.getConsultorioId());
        }
        validateConsultorioIdsForSede(sedeId, consultorioIds);
    }

    private void validateConsultorioIdsForSede(Integer sedeId, Set<UUID> consultorioIds) {
        for (UUID consultorioId : consultorioIds) {
            FacilityResource resource = facilityResourceJpaRepository.findById(consultorioId)
                    .orElseThrow(() -> new IllegalArgumentException("Consultorio not found: " + consultorioId));
            if (!resource.isActive()) {
                throw new IllegalArgumentException("Consultorio is not active: " + consultorioId);
            }
            if (!resource.getSedeId().equals(sedeId)) {
                throw new IllegalArgumentException(
                        "Consultorio " + consultorioId + " does not belong to sede " + sedeId);
            }
        }
    }

    private void validateSameConsultorioPerDayFromRequests(List<CreateSchedulePlanSlotRequest> slots) {
        Map<DayOfWeek, UUID> dailyConsultorio = new HashMap<>();
        for (CreateSchedulePlanSlotRequest slot : slots) {
            assertSameConsultorioForDay(dailyConsultorio, slot.dayOfWeek(), slot.consultorioId());
        }
    }

    private void validateSameConsultorioPerDayFromEntities(List<SchedulePlanSlot> slots) {
        Map<DayOfWeek, UUID> dailyConsultorio = new HashMap<>();
        for (SchedulePlanSlot slot : slots) {
            assertSameConsultorioForDay(dailyConsultorio, slot.getDayOfWeek(), slot.getConsultorioId());
        }
    }

    private void assertSameConsultorioForDay(Map<DayOfWeek, UUID> dailyConsultorio, DayOfWeek day, UUID consultorioId) {
        if (dailyConsultorio.containsKey(day)) {
            if (!dailyConsultorio.get(day).equals(consultorioId)) {
                throw new IllegalArgumentException(
                        "A doctor must work in the same consultorio for the entire day of " + day);
            }
        } else {
            dailyConsultorio.put(day, consultorioId);
        }
    }

    private void validateSlotTimeRangesFromRequests(List<CreateSchedulePlanSlotRequest> slots) {
        for (CreateSchedulePlanSlotRequest slot : slots) {
            if (!slot.endTime().isAfter(slot.startTime())) {
                throw new IllegalArgumentException("Slot end time must be after start time");
            }
        }
    }

    private void validateSlotTimeRangesFromEntities(List<SchedulePlanSlot> slots) {
        for (SchedulePlanSlot slot : slots) {
            if (!slot.getEndTime().isAfter(slot.getStartTime())) {
                throw new IllegalArgumentException("Slot end time must be after start time");
            }
        }
    }

    private void validateNoInternalSlotOverlapFromRequests(List<CreateSchedulePlanSlotRequest> slots) {
        for (int i = 0; i < slots.size(); i++) {
            CreateSchedulePlanSlotRequest left = slots.get(i);
            for (int j = i + 1; j < slots.size(); j++) {
                CreateSchedulePlanSlotRequest right = slots.get(j);
                if (left.dayOfWeek() != right.dayOfWeek()) {
                    continue;
                }
                if (timesOverlap(left.startTime(), left.endTime(), right.startTime(), right.endTime())) {
                    throw new IllegalArgumentException("Slot overlaps with another slot on " + left.dayOfWeek());
                }
            }
        }
    }

    private void validateNoInternalSlotOverlapFromEntities(List<SchedulePlanSlot> slots) {
        for (int i = 0; i < slots.size(); i++) {
            SchedulePlanSlot left = slots.get(i);
            for (int j = i + 1; j < slots.size(); j++) {
                SchedulePlanSlot right = slots.get(j);
                if (left.getDayOfWeek() != right.getDayOfWeek()) {
                    continue;
                }
                if (timesOverlap(
                        left.getStartTime(),
                        left.getEndTime(),
                        right.getStartTime(),
                        right.getEndTime()
                )) {
                    throw new IllegalArgumentException(
                            "Slot overlaps with another slot on " + left.getDayOfWeek());
                }
            }
        }
    }

    private boolean samePeriod(SchedulePlan plan, LocalDate startDate, LocalDate endDate) {
        return plan.getStartDate().equals(startDate) && plan.getEndDate().equals(endDate);
    }

    private boolean timesOverlap(LocalTime startA, LocalTime endA, LocalTime startB, LocalTime endB) {
        return startA.isBefore(endB) && startB.isBefore(endA);
    }
}
