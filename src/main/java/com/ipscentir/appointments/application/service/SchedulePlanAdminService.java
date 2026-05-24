package com.ipscentir.appointments.application.service;

import com.ipscentir.appointments.application.dto.schedule.CreateSchedulePlanBlockRequest;
import com.ipscentir.appointments.application.dto.schedule.CreateSchedulePlanRequest;
import com.ipscentir.appointments.application.dto.schedule.PublishSchedulePlanRequest;
import com.ipscentir.appointments.application.dto.schedule.SchedulePlanBlockDTO;
import com.ipscentir.appointments.application.dto.schedule.SchedulePlanDTO;
import com.ipscentir.appointments.application.dto.schedule.SchedulePlanPageResponse;
import com.ipscentir.appointments.application.dto.schedule.SchedulePlanSearchCriteria;
import com.ipscentir.appointments.application.dto.schedule.SchedulePlanSlotDTO;
import com.ipscentir.appointments.domain.model.schedule.SchedulePlan;
import com.ipscentir.appointments.domain.model.schedule.SchedulePlanBlock;
import com.ipscentir.appointments.domain.model.schedule.SchedulePlanSlot;
import com.ipscentir.appointments.domain.service.FacilityOperatingHoursService;
import com.ipscentir.appointments.infrastructure.persistence.jpa.SchedulePlanJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SchedulePlanAdminService {

    private final SchedulePlanJpaRepository schedulePlanJpaRepository;
    private final MedicoLookupService medicoLookupService;
    private final SchedulePlanMaterializationService schedulePlanMaterializationService;
    private final FacilityOperatingHoursService facilityOperatingHoursService;

    @Transactional
    public SchedulePlanDTO createPlan(CreateSchedulePlanRequest request) {
        var specialist = medicoLookupService.requireById(request.medicoId());

        if (request.slots() == null || request.slots().isEmpty()) {
            throw new IllegalArgumentException("Schedule plan must include at least one slot");
        }

        int nextVersion = schedulePlanJpaRepository.findMaxVersionBySpecialistAndPeriod(
            specialist.getId(),
            request.planYear(),
            request.planQuarter()
        ) + 1;

        SchedulePlan plan = SchedulePlan.builder()
            .specialistId(specialist.getId())
            .planYear(request.planYear())
            .planQuarter(request.planQuarter())
            .versionNumber(nextVersion)
            .published(false)
            .activeVersion(false)
            .build();

        request.slots().forEach(slotRequest -> {
            if (!slotRequest.endTime().isAfter(slotRequest.startTime())) {
                throw new IllegalArgumentException("Slot end time must be after start time");
            }
            validateSlotDoesNotOverlap(plan.getSlots(), slotRequest.dayOfWeek(), slotRequest.startTime(), slotRequest.endTime());
            facilityOperatingHoursService.assertSlotWithinInstitutionalHours(
                    slotRequest.dayOfWeek(),
                    slotRequest.startTime(),
                    slotRequest.endTime()
            );

            plan.addSlot(SchedulePlanSlot.builder()
                    .dayOfWeek(slotRequest.dayOfWeek())
                    .startTime(slotRequest.startTime())
                    .endTime(slotRequest.endTime())
                    .slotDurationMinutes(slotRequest.slotDurationMinutes())
                    .maxPatientsPerSlot(slotRequest.maxPatientsPerSlot())
                    .active(true)
                    .build());
        });

        SchedulePlan saved = schedulePlanJpaRepository.save(plan);
        return toDto(saved);
    }

    @Transactional
    public SchedulePlanDTO addBlock(UUID planId, CreateSchedulePlanBlockRequest request) {
        SchedulePlan plan = schedulePlanJpaRepository.findWithSlotsAndBlocksById(planId)
                .orElseThrow(() -> new IllegalArgumentException("Schedule plan not found"));

        if (plan.isPublished()) {
            throw new IllegalArgumentException("Cannot add blocks to a published schedule plan");
        }

        if (request.endDate().isBefore(request.startDate())) {
            throw new IllegalArgumentException("Block end date must be after or equal to start date");
        }

        if (!request.endTime().isAfter(request.startTime())) {
            throw new IllegalArgumentException("Block end time must be after start time");
        }

        facilityOperatingHoursService.assertBlockTimeWithinInstitutionalEnvelope(
                request.startTime(),
                request.endTime()
        );

        for (SchedulePlanBlock existing : plan.getBlocks()) {
            if (blocksOverlap(existing, request.startDate(), request.endDate(), request.startTime(), request.endTime())) {
                throw new IllegalArgumentException("Block overlaps with an existing block in this plan");
            }
        }

        plan.addBlock(SchedulePlanBlock.builder()
                .startDate(request.startDate())
                .endDate(request.endDate())
                .startTime(request.startTime())
                .endTime(request.endTime())
                .reason(request.reason())
                .createdBy(request.createdBy())
                .build());

        return toDto(schedulePlanJpaRepository.save(plan));
    }

    @Transactional
    public SchedulePlanDTO publish(UUID planId, PublishSchedulePlanRequest request) {
        SchedulePlan plan = schedulePlanJpaRepository.findWithSlotsAndBlocksById(planId)
                .orElseThrow(() -> new IllegalArgumentException("Schedule plan not found"));

        if (plan.isPublished()) {
            throw new IllegalArgumentException("Schedule plan is already published");
        }

        boolean hasActiveSlot = plan.getSlots().stream().anyMatch(SchedulePlanSlot::isActive);
        if (!hasActiveSlot) {
            throw new IllegalArgumentException("Cannot publish a schedule plan without active slots");
        }

        facilityOperatingHoursService.assertPlanSlotsWithinSedeHours(request.sedeId(), plan);
        facilityOperatingHoursService.assertPlanBlocksWithinSedeHours(request.sedeId(), plan);

        schedulePlanJpaRepository.findBySpecialistIdAndPlanYearAndPlanQuarterAndActiveVersionTrue(
                plan.getSpecialistId(),
                plan.getPlanYear(),
                plan.getPlanQuarter()
        ).ifPresent(activePlan -> {
            if (!activePlan.getId().equals(plan.getId())) {
                activePlan.markAsInactiveVersion();
                schedulePlanJpaRepository.save(activePlan);
            }
        });

        plan.publishAsActive();
        SchedulePlan saved = schedulePlanJpaRepository.save(plan);
        schedulePlanMaterializationService.materializePublishedPlan(saved, request.sedeId());
        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public SchedulePlanDTO getById(UUID planId) {
        SchedulePlan plan = schedulePlanJpaRepository.findWithSlotsAndBlocksById(planId)
                .orElseThrow(() -> new IllegalArgumentException("Schedule plan not found"));

        return toDto(plan);
    }

    @Transactional(readOnly = true)
    public List<SchedulePlanDTO> listByMedico(String medicoId, Integer year, Integer quarter) {
        medicoLookupService.requireById(medicoId);

        List<SchedulePlan> plans;
        if (year != null && quarter != null) {
            plans = schedulePlanJpaRepository.findBySpecialistIdAndPlanYearAndPlanQuarterOrderByVersionNumberDesc(
                    medicoId,
                    year,
                    quarter
            );
        } else {
            plans = schedulePlanJpaRepository.findBySpecialistIdOrderByPlanYearDescPlanQuarterDescVersionNumberDesc(medicoId);
        }

        return plans.stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public SchedulePlanPageResponse search(SchedulePlanSearchCriteria criteria) {
        if (criteria.medicoId() == null || criteria.medicoId().isBlank()) {
            throw new IllegalArgumentException("medicoId is required for schedule plan search");
        }

        List<SchedulePlanDTO> all = listByMedico(criteria.medicoId(), criteria.year(), criteria.quarter())
                .stream()
                .filter(dto -> criteria.published() == null || dto.published() == criteria.published())
                .filter(dto -> criteria.activeVersion() == null || dto.activeVersion() == criteria.activeVersion())
                .sorted(Comparator
                        .comparing(SchedulePlanDTO::planYear).reversed()
                        .thenComparing(SchedulePlanDTO::planQuarter).reversed()
                        .thenComparing(SchedulePlanDTO::versionNumber).reversed())
                .toList();

        int total = all.size();
        int totalPages = total == 0 ? 0 : (int) Math.ceil((double) total / criteria.size());
        int fromIndex = Math.min(criteria.page() * criteria.size(), total);
        int toIndex = Math.min(fromIndex + criteria.size(), total);

        return new SchedulePlanPageResponse(
                all.subList(fromIndex, toIndex),
                criteria.page(),
                criteria.size(),
                total,
                totalPages
        );
    }

    private void validateSlotDoesNotOverlap(
            List<SchedulePlanSlot> existingSlots,
            java.time.DayOfWeek dayOfWeek,
            java.time.LocalTime start,
            java.time.LocalTime end
    ) {
        for (SchedulePlanSlot slot : existingSlots) {
            if (!slot.isActive() || slot.getDayOfWeek() != dayOfWeek) {
                continue;
            }
            if (start.isBefore(slot.getEndTime()) && slot.getStartTime().isBefore(end)) {
                throw new IllegalArgumentException("Slot overlaps with another slot on " + dayOfWeek);
            }
        }
    }

    private boolean blocksOverlap(
            SchedulePlanBlock existing,
            java.time.LocalDate startDate,
            java.time.LocalDate endDate,
            java.time.LocalTime startTime,
            java.time.LocalTime endTime
    ) {
        if (existing.getEndDate().isBefore(startDate) || endDate.isBefore(existing.getStartDate())) {
            return false;
        }
        return startTime.isBefore(existing.getEndTime()) && existing.getStartTime().isBefore(endTime);
    }

    private SchedulePlanDTO toDto(SchedulePlan plan) {
        List<SchedulePlanSlotDTO> slots = plan.getSlots().stream()
                .map(slot -> new SchedulePlanSlotDTO(
                        slot.getId(),
                        slot.getDayOfWeek(),
                        slot.getStartTime(),
                        slot.getEndTime(),
                        slot.getSlotDurationMinutes(),
                        slot.getMaxPatientsPerSlot(),
                        slot.isActive()
                ))
                .toList();

        List<SchedulePlanBlockDTO> blocks = plan.getBlocks().stream()
                .map(block -> new SchedulePlanBlockDTO(
                        block.getId(),
                        block.getStartDate(),
                        block.getEndDate(),
                        block.getStartTime(),
                        block.getEndTime(),
                        block.getReason(),
                        block.getCreatedBy()
                ))
                .toList();

        return new SchedulePlanDTO(
                plan.getId(),
            plan.getSpecialistId(),
                plan.getPlanYear(),
                plan.getPlanQuarter(),
                plan.getVersionNumber(),
                plan.isPublished(),
                plan.isActiveVersion(),
                plan.getPublishedAt(),
                slots,
                blocks
        );
    }
}
