package com.ipscentir.appointments.application.service;

import com.ipscentir.appointments.domain.model.schedule.Schedule;
import com.ipscentir.appointments.domain.model.schedule.SchedulePlan;
import com.ipscentir.appointments.domain.model.schedule.SchedulePlanSlot;
import com.ipscentir.appointments.domain.repository.ScheduleRepository;
import com.ipscentir.appointments.domain.service.FacilityOperatingHoursService;
import com.ipscentir.appointments.infrastructure.persistence.jpa.SpecialistJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SchedulePlanMaterializationService {

    private final ScheduleRepository scheduleRepository;
    private final SpecialistJpaRepository specialistJpaRepository;
    private final FacilityOperatingHoursService facilityOperatingHoursService;

    @Transactional
    public void materializePublishedPlan(SchedulePlan plan, Integer sedeId) {
        var specialist = specialistJpaRepository.findById(plan.getSpecialistId())
                .orElseThrow(() -> new IllegalArgumentException("Specialist not found"));

        String specialty = specialist.getSpecialty();

        for (SchedulePlanSlot slot : plan.getSlots()) {
            if (!slot.isActive()) {
                continue;
            }

            facilityOperatingHoursService.assertSlotWithinSedeHours(
                    sedeId,
                    slot.getDayOfWeek(),
                    slot.getStartTime(),
                    slot.getEndTime()
            );

            Schedule schedule = scheduleRepository
                    .findByDoctorIdAndSedeIdAndDayOfWeek(plan.getSpecialistId(), sedeId, slot.getDayOfWeek())
                    .orElseGet(() -> Schedule.builder()
                            .doctorId(plan.getSpecialistId())
                            .sedeId(sedeId)
                            .specialty(specialty)
                            .dayOfWeek(slot.getDayOfWeek())
                            .startTime(slot.getStartTime())
                            .endTime(slot.getEndTime())
                            .slotDurationMinutes(slot.getSlotDurationMinutes())
                            .maxPatientsPerSlot(slot.getMaxPatientsPerSlot())
                            .isActive(true)
                            .build());

            schedule.applyPublishedSlotConfiguration(
                    specialty,
                    slot.getStartTime(),
                    slot.getEndTime(),
                    slot.getSlotDurationMinutes(),
                    slot.getMaxPatientsPerSlot()
            );
            scheduleRepository.save(schedule);
        }
    }
}
