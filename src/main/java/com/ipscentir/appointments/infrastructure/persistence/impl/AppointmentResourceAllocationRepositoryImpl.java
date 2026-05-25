package com.ipscentir.appointments.infrastructure.persistence.impl;

import com.ipscentir.appointments.domain.model.facility.AppointmentResourceAllocation;
import com.ipscentir.appointments.domain.model.facility.FacilityResourceType;
import com.ipscentir.appointments.domain.repository.AppointmentResourceAllocationRepository;
import com.ipscentir.appointments.infrastructure.persistence.jpa.AppointmentResourceAllocationJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AppointmentResourceAllocationRepositoryImpl implements AppointmentResourceAllocationRepository {

    private final AppointmentResourceAllocationJpaRepository jpaRepository;

    @Override
    public Optional<AppointmentResourceAllocation> findActiveByAppointmentId(UUID appointmentId) {
        return jpaRepository.findByAppointmentIdAndReleasedAtIsNull(appointmentId);
    }

    @Override
    public boolean existsActiveSessionKey(String capacitySessionKey) {
        return jpaRepository.existsByCapacitySessionKeyAndReleasedAtIsNull(capacitySessionKey);
    }

    @Override
    public long countOccupiedCapacityUnits(
            Integer sedeId,
            FacilityResourceType resourceType,
            LocalDate appointmentDate,
            LocalTime startTime,
            LocalTime endTime,
            UUID excludeAppointmentId) {
        if (excludeAppointmentId == null) {
            return jpaRepository.countOccupiedCapacityUnits(
                    sedeId,
                    resourceType,
                    appointmentDate,
                    startTime,
                    endTime);
        }

        return jpaRepository.countOccupiedCapacityUnitsExcludingAppointment(
                sedeId,
                resourceType,
                appointmentDate,
                startTime,
                endTime,
                excludeAppointmentId);
    }

    @Override
    public long countOccupiedForResource(
            UUID facilityResourceId,
            LocalDate appointmentDate,
            LocalTime startTime,
            LocalTime endTime,
            UUID excludeAppointmentId) {
        return jpaRepository.countOccupiedForResource(
                facilityResourceId, appointmentDate, startTime, endTime, excludeAppointmentId);
    }

    @Override
    public AppointmentResourceAllocation save(AppointmentResourceAllocation allocation) {
        return jpaRepository.save(allocation);
    }
}
