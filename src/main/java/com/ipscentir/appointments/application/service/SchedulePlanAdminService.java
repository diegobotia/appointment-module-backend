package com.ipscentir.appointments.application.service;

import com.ipscentir.appointments.application.dto.schedule.CreateSchedulePlanBlockRequest;
import com.ipscentir.appointments.application.dto.schedule.CreateSchedulePlanRequest;
import com.ipscentir.appointments.application.dto.schedule.SchedulePlanBlockDTO;
import com.ipscentir.appointments.application.dto.schedule.SchedulePlanDTO;
import com.ipscentir.appointments.application.dto.schedule.SchedulePlanSlotDTO;
import com.ipscentir.appointments.domain.model.schedule.SchedulePlan;
import com.ipscentir.appointments.domain.model.schedule.SchedulePlanBlock;
import com.ipscentir.appointments.domain.model.schedule.SchedulePlanSlot;
import com.ipscentir.appointments.infrastructure.persistence.jpa.SchedulePlanJpaRepository;
import com.ipscentir.appointments.infrastructure.persistence.jpa.SpecialistJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SchedulePlanAdminService {

    private final SchedulePlanJpaRepository schedulePlanJpaRepository;
    private final SpecialistJpaRepository specialistJpaRepository;

    @Transactional
    public SchedulePlanDTO createPlan(CreateSchedulePlanRequest request) {
        var specialist = specialistJpaRepository.findById(request.specialistId())
            .orElseThrow(() -> new IllegalArgumentException("Specialist not found"));

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

        if (request.endDate().isBefore(request.startDate())) {
            throw new IllegalArgumentException("Block end date must be after or equal to start date");
        }

        if (!request.endTime().isAfter(request.startTime())) {
            throw new IllegalArgumentException("Block end time must be after start time");
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
    public SchedulePlanDTO publish(UUID planId) {
        SchedulePlan plan = schedulePlanJpaRepository.findWithSlotsAndBlocksById(planId)
                .orElseThrow(() -> new IllegalArgumentException("Schedule plan not found"));

        schedulePlanJpaRepository.findBySpecialistIdAndPlanYearAndPlanQuarterAndActiveVersionTrue(
                plan.getSpecialistId(),
                        plan.getPlanYear(),
                        plan.getPlanQuarter()
                )
                .ifPresent(activePlan -> {
                    if (!activePlan.getId().equals(plan.getId())) {
                        activePlan.markAsInactiveVersion();
                        schedulePlanJpaRepository.save(activePlan);
                    }
                });

        plan.publishAsActive();
        return toDto(schedulePlanJpaRepository.save(plan));
    }

    @Transactional(readOnly = true)
    public SchedulePlanDTO getById(UUID planId) {
        SchedulePlan plan = schedulePlanJpaRepository.findWithSlotsAndBlocksById(planId)
                .orElseThrow(() -> new IllegalArgumentException("Schedule plan not found"));

        return toDto(plan);
    }

    @Transactional(readOnly = true)
    public List<SchedulePlanDTO> listBySpecialist(String specialistId, Integer year, Integer quarter) {
        specialistJpaRepository.findById(specialistId)
                .orElseThrow(() -> new IllegalArgumentException("Specialist not found"));

        List<SchedulePlan> plans;
        if (year != null && quarter != null) {
            plans = schedulePlanJpaRepository.findBySpecialistIdAndPlanYearAndPlanQuarterOrderByVersionNumberDesc(
                    specialistId,
                    year,
                    quarter
            );
        } else {
            plans = schedulePlanJpaRepository.findBySpecialistIdOrderByPlanYearDescPlanQuarterDescVersionNumberDesc(specialistId);
        }

        return plans.stream().map(this::toDto).toList();
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
