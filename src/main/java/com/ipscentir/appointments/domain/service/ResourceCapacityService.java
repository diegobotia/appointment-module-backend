package com.ipscentir.appointments.domain.service;

import com.ipscentir.appointments.application.exception.ResourceCapacityExceededException;
import com.ipscentir.appointments.domain.model.appointment.Appointment;
import com.ipscentir.appointments.domain.model.appointment.AppointmentType;
import com.ipscentir.appointments.domain.model.catalog.AppointmentServiceType;
import com.ipscentir.appointments.domain.model.facility.AppointmentResourceAllocation;
import com.ipscentir.appointments.domain.model.facility.FacilityResource;
import com.ipscentir.appointments.domain.model.facility.FacilityResourceType;
import com.ipscentir.appointments.domain.repository.AppointmentResourceAllocationRepository;
import com.ipscentir.appointments.domain.model.sede.Sede;
import com.ipscentir.appointments.domain.repository.SedeRepository;
import com.ipscentir.appointments.domain.repository.FacilityResourceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ResourceCapacityService {

    private final FacilityResourceRepository facilityResourceRepository;
    private final SedeRepository sedeRepository;
    private final AppointmentResourceAllocationRepository allocationRepository;

    @Transactional(readOnly = true)
    public boolean hasPhysicalCapacity(
            Integer sedeId,
            AppointmentType appointmentType,
            UUID scheduleId,
            LocalDate date,
            LocalTime startTime,
            int durationMinutes,
            UUID excludeAppointmentId
    ) {
        FacilityResourceType resourceType = AppointmentResourceTypeResolver.forAppointmentType(appointmentType);
        return canAllocate(sedeId, resourceType, appointmentType, scheduleId, date, startTime, durationMinutes, excludeAppointmentId);
    }

    @Transactional(readOnly = true)
    public boolean hasPhysicalCapacityForService(
            Integer sedeId,
            AppointmentServiceType serviceType,
            UUID scheduleId,
            LocalDate date,
            LocalTime startTime,
            int durationMinutes
    ) {
        FacilityResourceType resourceType = AppointmentResourceTypeResolver.forServiceType(serviceType);
        AppointmentType appointmentType = mapServiceToAppointmentType(serviceType);
        return canAllocate(sedeId, resourceType, appointmentType, scheduleId, date, startTime, durationMinutes, null);
    }

    @Transactional(readOnly = true)
    public void assertCanAllocate(
            Integer sedeId,
            AppointmentType appointmentType,
            UUID scheduleId,
            LocalDate date,
            LocalTime startTime,
            int durationMinutes,
            UUID excludeAppointmentId
    ) {
        assertCanAllocate(sedeId, appointmentType, scheduleId, date, startTime, durationMinutes, excludeAppointmentId, null);
    }

    @Transactional(readOnly = true)
    public void assertCanAllocate(
            Integer sedeId,
            AppointmentType appointmentType,
            UUID scheduleId,
            LocalDate date,
            LocalTime startTime,
            int durationMinutes,
            UUID excludeAppointmentId,
            UUID facilityResourceId
    ) {
        FacilityResourceType resourceType = AppointmentResourceTypeResolver.forAppointmentType(appointmentType);
        int totalUnits = totalUnits(sedeId, resourceType);
        if (totalUnits == 0) return;

        LocalTime endTime = endTime(startTime, durationMinutes);
        String sessionKey = capacitySessionKey(sedeId, appointmentType, scheduleId, date, startTime, null);

        if (isTherapy(appointmentType) && allocationRepository.existsActiveSessionKey(sessionKey)) return;

        long occupied;
        if (facilityResourceId != null) {
            occupied = allocationRepository.countOccupiedForResource(
                    facilityResourceId, date, startTime, endTime, excludeAppointmentId);
            Optional<FacilityResource> resource = facilityResourceRepository.findById(facilityResourceId);
            int capacity = resource.map(FacilityResource::getCapacityUnits).orElse(0);
            if (occupied >= capacity) {
                throw buildCapacityException(sedeId, resourceType, appointmentType, date, startTime, durationMinutes, excludeAppointmentId);
            }
            return;
        }

        occupied = countOccupiedWithLock(sedeId, resourceType, date, startTime, endTime, excludeAppointmentId);
        if (occupied >= totalUnits) {
            throw buildCapacityException(sedeId, resourceType, appointmentType, date, startTime, durationMinutes, excludeAppointmentId);
        }
    }

    private long countOccupiedWithLock(Integer sedeId, FacilityResourceType resourceType, LocalDate date, LocalTime startTime, LocalTime endTime, UUID excludeAppointmentId) {
        List<AppointmentResourceAllocation> allocations = allocationRepository.findOccupiedForUpdate(
                sedeId, resourceType, date, startTime, endTime);
        if (excludeAppointmentId != null) {
            allocations = allocations.stream()
                    .filter(a -> !a.getAppointmentId().equals(excludeAppointmentId))
                    .toList();
        }
        return allocations.stream()
                .map(AppointmentResourceAllocation::getCapacitySessionKey)
                .distinct()
                .count();
    }

    public void allocate(Appointment appointment) {
        allocate(appointment, null);
    }

    public void allocate(Appointment appointment, UUID facilityResourceId) {
        FacilityResourceType resourceType = AppointmentResourceTypeResolver.forAppointmentType(appointment.getAppointmentType());
        int totalUnits = totalUnits(sedeId(appointment), resourceType);
        if (totalUnits == 0) {
            return;
        }

        if (facilityResourceId != null) {
            Optional<FacilityResource> resource = facilityResourceRepository.findById(facilityResourceId);
            if (resource.isEmpty() || !resource.get().isActive()) {
                return;
            }
        }

        String sessionKey = capacitySessionKey(
                appointment.getSedeId(),
                appointment.getAppointmentType(),
                appointment.getScheduleId(),
                appointment.getAppointmentDate(),
                appointment.getAppointmentTime(),
                appointment.getId()
        );

        LocalTime endTime = endTime(appointment.getAppointmentTime(), appointment.getDurationMinutes());

        allocationRepository.findByAppointmentId(appointment.getId()).ifPresentOrElse(
                existing -> {
                    existing.updateAllocation(
                            appointment.getSedeId(),
                            resourceType,
                            facilityResourceId,
                            appointment.getAppointmentDate(),
                            appointment.getAppointmentTime(),
                            endTime,
                            sessionKey
                    );
                    allocationRepository.save(existing);
                },
                () -> allocationRepository.save(AppointmentResourceAllocation.builder()
                        .appointmentId(appointment.getId())
                        .sedeId(appointment.getSedeId())
                        .resourceType(resourceType)
                        .facilityResourceId(facilityResourceId)
                        .appointmentDate(appointment.getAppointmentDate())
                        .startTime(appointment.getAppointmentTime())
                        .endTime(endTime)
                        .capacitySessionKey(sessionKey)
                        .build())
        );
    }

    public void release(UUID appointmentId) {
        allocationRepository.findActiveByAppointmentId(appointmentId).ifPresent(allocation -> {
            allocation.release();
            allocationRepository.save(allocation);
        });
    }

    public void reallocate(Appointment appointment) {
        release(appointment.getId());
        assertCanAllocate(
                appointment.getSedeId(),
                appointment.getAppointmentType(),
                appointment.getScheduleId(),
                appointment.getAppointmentDate(),
                appointment.getAppointmentTime(),
                appointment.getDurationMinutes(),
                appointment.getId()
        );
        allocate(appointment);
    }

    public void reallocate(Appointment appointment, UUID facilityResourceId) {
        release(appointment.getId());
        assertCanAllocate(
                appointment.getSedeId(),
                appointment.getAppointmentType(),
                appointment.getScheduleId(),
                appointment.getAppointmentDate(),
                appointment.getAppointmentTime(),
                appointment.getDurationMinutes(),
                appointment.getId(),
                facilityResourceId
        );
        allocate(appointment, facilityResourceId);
    }

    private boolean canAllocate(
            Integer sedeId,
            FacilityResourceType resourceType,
            AppointmentType appointmentType,
            UUID scheduleId,
            LocalDate date,
            LocalTime startTime,
            int durationMinutes,
            UUID excludeAppointmentId
    ) {
        return canAllocate(sedeId, resourceType, appointmentType, scheduleId, date, startTime,
                durationMinutes, excludeAppointmentId, null);
    }

    private boolean canAllocate(
            Integer sedeId,
            FacilityResourceType resourceType,
            AppointmentType appointmentType,
            UUID scheduleId,
            LocalDate date,
            LocalTime startTime,
            int durationMinutes,
            UUID excludeAppointmentId,
            UUID facilityResourceId
    ) {
        LocalTime endTime = endTime(startTime, durationMinutes);
        String sessionKey = capacitySessionKey(sedeId, appointmentType, scheduleId, date, startTime, null);

        int totalUnits = totalUnits(sedeId, resourceType);

        if (isTherapy(appointmentType) && allocationRepository.existsActiveSessionKey(sessionKey)) {
            return true;
        }
        if (totalUnits == 0) {
            return true;
        }

        if (facilityResourceId != null) {
            Optional<FacilityResource> resource = facilityResourceRepository.findById(facilityResourceId);
            if (resource.isEmpty() || !resource.get().isActive()) {
                return false;
            }
            long occupied = allocationRepository.countOccupiedForResource(
                    facilityResourceId, date, startTime, endTime, excludeAppointmentId
            );
            return occupied < resource.get().getCapacityUnits();
        }

        long occupied = allocationRepository.countOccupiedCapacityUnits(
                sedeId, resourceType, date, startTime, endTime, excludeAppointmentId
        );
        return occupied < totalUnits;
    }

    private ResourceCapacityExceededException buildCapacityException(
            Integer sedeId,
            FacilityResourceType resourceType,
            AppointmentType appointmentType,
            LocalDate date,
            LocalTime startTime,
            int durationMinutes,
            UUID excludeAppointmentId
    ) {
        LocalTime endTime = endTime(startTime, durationMinutes);
        long occupied = allocationRepository.countOccupiedCapacityUnits(
                sedeId, resourceType, date, startTime, endTime, excludeAppointmentId
        );
        int totalUnits = totalUnits(sedeId, resourceType);
        String sedeNombre = sedeRepository.findById(sedeId)
                .map(Sede::getNombre)
                .orElse("sede");

        String resourceLabel = switch (resourceType) {
            case CONSULTORIO -> "consultorios";
            case FISIOTERAPIA -> "salas de fisioterapia";
            case TERAPIA_OCUPACIONAL -> "salas de terapia ocupacional";
            case REUNION_STAFF -> "salas de reunión";
            case SIN_RECURSO -> "sin recurso físico";
        };

        return new ResourceCapacityExceededException(
                "No hay " + resourceLabel + " disponibles en " + sedeNombre
                        + " para la franja " + startTime + "–" + endTime
                        + " del " + date
                        + " (ocupación " + occupied + "/" + totalUnits + ")",
                sedeId,
                resourceType,
                date,
                startTime,
                endTime,
                totalUnits,
                occupied
        );
    }

    private int totalUnits(Integer sedeId, FacilityResourceType resourceType) {
        return (int) facilityResourceRepository.countActiveBySedeIdAndResourceType(sedeId, resourceType);
    }

    private Integer sedeId(Appointment appointment) {
        return appointment.getSedeId();
    }

    private LocalTime endTime(LocalTime start, int durationMinutes) {
        return start.plusMinutes(durationMinutes);
    }

    private String capacitySessionKey(
            Integer sedeId,
            AppointmentType appointmentType,
            UUID scheduleId,
            LocalDate date,
            LocalTime time,
            UUID appointmentId
    ) {
        if (isTherapy(appointmentType) && scheduleId != null) {
            return sedeId + "|" + date + "|" + time + "|" + appointmentType + "|" + scheduleId;
        }
        return sedeId + "|" + appointmentId;
    }

    private boolean isTherapy(AppointmentType type) {
        return type == AppointmentType.TERAPIA_FISICA || type == AppointmentType.TERAPIA_OCUPACIONAL;
    }

    private AppointmentType mapServiceToAppointmentType(AppointmentServiceType serviceType) {
        return switch (serviceType) {
            case TERAPIA_FISICA -> AppointmentType.TERAPIA_FISICA;
            case TERAPIA_OCUPACIONAL -> AppointmentType.TERAPIA_OCUPACIONAL;
            case JUNTA_MEDICA -> AppointmentType.JUNTA_MEDICA;
            case STAFF -> AppointmentType.STAFF;
            default -> AppointmentType.PRESENCIAL;
        };
    }
}
