package com.ipscentir.appointments.application.service;

import com.ipscentir.appointments.application.dto.schedule.CreateSchedulePlanBlockRequest;
import com.ipscentir.appointments.application.dto.schedule.CreateSchedulePlanRequest;
import com.ipscentir.appointments.application.dto.schedule.CreateSchedulePlanSlotRequest;
import com.ipscentir.appointments.application.dto.schedule.PublishSchedulePlanRequest;
import com.ipscentir.appointments.application.dto.schedule.SchedulePlanBlockDTO;
import com.ipscentir.appointments.application.dto.schedule.SchedulePlanDTO;
import com.ipscentir.appointments.application.dto.schedule.SchedulePlanPageResponse;
import com.ipscentir.appointments.application.dto.schedule.SchedulePlanSearchCriteria;
import com.ipscentir.appointments.application.dto.schedule.SchedulePlanSlotDTO;
import com.ipscentir.appointments.application.dto.schedule.UpdateSchedulePlanRequest;
import com.ipscentir.appointments.domain.model.schedule.SchedulePlan;
import com.ipscentir.appointments.domain.model.schedule.SchedulePlanBlock;
import com.ipscentir.appointments.domain.model.schedule.SchedulePlanSlot;
import com.ipscentir.appointments.domain.service.FacilityOperatingHoursService;
import com.ipscentir.appointments.domain.service.SchedulePlanRulesValidator;
import com.ipscentir.appointments.infrastructure.persistence.jpa.SchedulePlanJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
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
    private final SchedulePlanRulesValidator schedulePlanRulesValidator;

    @Transactional
    public SchedulePlanDTO createPlan(CreateSchedulePlanRequest request) {
        var specialist = medicoLookupService.requireById(request.medicoId());

        schedulePlanRulesValidator.validateDuration(request.startDate(), request.endDate());
        schedulePlanRulesValidator.validateSlotsForCreation(request.slots());
        schedulePlanRulesValidator.validateConsultoriosBelongToSede(request.sedeId(), request.slots());
        schedulePlanRulesValidator.validateNoConsultorioOverlapWithOtherDoctors(
                specialist.getId(),
                request.startDate(),
                request.endDate(),
                request.slots()
        );

        for (CreateSchedulePlanSlotRequest slotRequest : request.slots()) {
            facilityOperatingHoursService.assertSlotWithinSedeHours(
                    request.sedeId(),
                    slotRequest.dayOfWeek(),
                    slotRequest.startTime(),
                    slotRequest.endTime()
            );
        }

        int nextVersion = schedulePlanJpaRepository.findMaxVersionBySpecialistAndPeriod(
                specialist.getId(),
                request.startDate(),
                request.endDate()
        ) + 1;

        SchedulePlan plan = SchedulePlan.builder()
                .specialistId(specialist.getId())
                .sedeId(request.sedeId())
                .startDate(request.startDate())
                .endDate(request.endDate())
                .versionNumber(nextVersion)
                .published(false)
                .activeVersion(false)
                .build();

        request.slots().forEach(slotRequest -> plan.addSlot(SchedulePlanSlot.builder()
                .dayOfWeek(slotRequest.dayOfWeek())
                .startTime(slotRequest.startTime())
                .endTime(slotRequest.endTime())
                .slotDurationMinutes(slotRequest.slotDurationMinutes())
                .maxPatientsPerSlot(slotRequest.maxPatientsPerSlot())
                .consultorioId(slotRequest.consultorioId())
                .active(true)
                .build()));

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

        if (!plan.getSedeId().equals(request.sedeId())) {
            throw new IllegalArgumentException(
                    "Publish sedeId must match the sede configured on the schedule plan");
        }

        schedulePlanRulesValidator.validateSlotsForPublication(plan);
        schedulePlanRulesValidator.validateNoActiveDoctorOverlapOnPublish(plan);
        schedulePlanRulesValidator.validateNoConsultorioOverlapWithOtherDoctorsOnPublish(plan);

        facilityOperatingHoursService.assertPlanSlotsWithinSedeHours(plan.getSedeId(), plan);
        facilityOperatingHoursService.assertPlanBlocksWithinSedeHours(plan.getSedeId(), plan);

        schedulePlanJpaRepository.findBySpecialistIdAndStartDateAndEndDateAndActiveVersionTrue(
                plan.getSpecialistId(),
                plan.getStartDate(),
                plan.getEndDate()
        ).ifPresent(activePlan -> {
            if (!activePlan.getId().equals(plan.getId())) {
                activePlan.markAsInactiveVersion();
                schedulePlanJpaRepository.save(activePlan);
            }
        });

        plan.publishAsActive();
        SchedulePlan saved = schedulePlanJpaRepository.save(plan);
        schedulePlanMaterializationService.materializePublishedPlan(saved);
        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public SchedulePlanDTO getById(UUID planId) {
        SchedulePlan plan = schedulePlanJpaRepository.findWithSlotsAndBlocksById(planId)
                .orElseThrow(() -> new IllegalArgumentException("Schedule plan not found"));

        return toDto(plan);
    }

    @Transactional(readOnly = true)
    public List<SchedulePlanDTO> listByMedico(String medicoId, LocalDate startDate, LocalDate endDate) {
        medicoLookupService.requireById(medicoId);

        List<SchedulePlan> plans;
        if (startDate != null && endDate != null) {
            plans = schedulePlanJpaRepository.findBySpecialistIdAndStartDateAndEndDateOrderByVersionNumberDesc(
                    medicoId,
                    startDate,
                    endDate
            );
        } else {
            plans = schedulePlanJpaRepository.findBySpecialistIdOrderByStartDateDescEndDateDescVersionNumberDesc(medicoId);
        }

        return plans.stream().map(this::toDto).toList();
    }

    @Transactional
    public SchedulePlanDTO updatePlan(UUID planId, UpdateSchedulePlanRequest request) {
        SchedulePlan plan = schedulePlanJpaRepository.findWithSlotsAndBlocksById(planId)
                .orElseThrow(() -> new IllegalArgumentException("Schedule plan not found"));

        if (plan.isPublished()) {
            throw new IllegalArgumentException("Cannot update a published schedule plan. Unpublish it first.");
        }

        var specialist = medicoLookupService.requireById(request.medicoId());

        schedulePlanRulesValidator.validateDuration(request.startDate(), request.endDate());

        if (request.slots() != null && !request.slots().isEmpty()) {
            schedulePlanRulesValidator.validateSlotsForCreation(request.slots());
            schedulePlanRulesValidator.validateConsultoriosBelongToSede(request.sedeId(), request.slots());
            schedulePlanRulesValidator.validateNoConsultorioOverlapWithOtherDoctors(
                    specialist.getId(),
                    request.startDate(),
                    request.endDate(),
                    request.slots()
            );
            for (CreateSchedulePlanSlotRequest slotRequest : request.slots()) {
                facilityOperatingHoursService.assertSlotWithinSedeHours(
                        request.sedeId(),
                        slotRequest.dayOfWeek(),
                        slotRequest.startTime(),
                        slotRequest.endTime()
                );
            }

            List<SchedulePlanSlot> newSlots = request.slots().stream()
                    .map(sr -> SchedulePlanSlot.builder()
                            .dayOfWeek(sr.dayOfWeek())
                            .startTime(sr.startTime())
                            .endTime(sr.endTime())
                            .slotDurationMinutes(sr.slotDurationMinutes())
                            .maxPatientsPerSlot(sr.maxPatientsPerSlot())
                            .consultorioId(sr.consultorioId())
                            .active(true)
                            .build())
                    .toList();
            plan.replaceSlots(newSlots);
        }

        plan.setSpecialistId(specialist.getId());
        plan.setSedeId(request.sedeId());
        plan.updateDates(request.startDate(), request.endDate());
        return toDto(schedulePlanJpaRepository.save(plan));
    }

    @Transactional
    public SchedulePlanDTO deleteSlot(UUID planId, UUID slotId) {
        SchedulePlan plan = schedulePlanJpaRepository.findWithSlotsAndBlocksById(planId)
                .orElseThrow(() -> new IllegalArgumentException("Schedule plan not found"));

        if (plan.isPublished()) {
            throw new IllegalArgumentException("Cannot modify a published schedule plan. Unpublish it first.");
        }

        if (!plan.removeSlot(slotId)) {
            throw new IllegalArgumentException("Slot not found in plan: " + slotId);
        }

        return toDto(schedulePlanJpaRepository.save(plan));
    }

    @Transactional
    public SchedulePlanDTO deleteBlock(UUID planId, UUID blockId) {
        SchedulePlan plan = schedulePlanJpaRepository.findWithSlotsAndBlocksById(planId)
                .orElseThrow(() -> new IllegalArgumentException("Schedule plan not found"));

        if (plan.isPublished()) {
            throw new IllegalArgumentException("Cannot modify a published schedule plan. Unpublish it first.");
        }

        if (!plan.removeBlock(blockId)) {
            throw new IllegalArgumentException("Block not found in plan: " + blockId);
        }

        return toDto(schedulePlanJpaRepository.save(plan));
    }

    @Transactional
    public SchedulePlanDTO unpublish(UUID planId) {
        SchedulePlan plan = schedulePlanJpaRepository.findWithSlotsAndBlocksById(planId)
                .orElseThrow(() -> new IllegalArgumentException("Schedule plan not found"));

        if (!plan.isPublished()) {
            throw new IllegalArgumentException("Schedule plan is not published");
        }

        plan.unpublish();
        return toDto(schedulePlanJpaRepository.save(plan));
    }

    @Transactional(readOnly = true)
    public SchedulePlanPageResponse search(SchedulePlanSearchCriteria criteria) {
        if (criteria.medicoId() == null || criteria.medicoId().isBlank()) {
            throw new IllegalArgumentException("medicoId is required for schedule plan search");
        }

        List<SchedulePlanDTO> all = listByMedico(criteria.medicoId(), criteria.startDate(), criteria.endDate())
                .stream()
                .filter(dto -> criteria.published() == null || dto.published() == criteria.published())
                .filter(dto -> criteria.activeVersion() == null || dto.activeVersion() == criteria.activeVersion())
                .sorted(Comparator
                        .comparing(SchedulePlanDTO::startDate).reversed()
                        .thenComparing(SchedulePlanDTO::endDate).reversed()
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

    private boolean blocksOverlap(
            SchedulePlanBlock existing,
            LocalDate startDate,
            LocalDate endDate,
            LocalTime startTime,
            LocalTime endTime
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
                        slot.getConsultorioId(),
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
                plan.getSedeId(),
                plan.getStartDate(),
                plan.getEndDate(),
                plan.getVersionNumber(),
                plan.isPublished(),
                plan.isActiveVersion(),
                plan.getPublishedAt(),
                slots,
                blocks
        );
    }
}
