package com.ipscentir.appointments.domain.repository;

import com.ipscentir.appointments.domain.model.facility.AppointmentResourceAllocation;
import com.ipscentir.appointments.domain.model.facility.FacilityResourceType;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;

public interface AppointmentResourceAllocationRepository {

    Optional<AppointmentResourceAllocation> findActiveByAppointmentId(UUID appointmentId);

    boolean existsActiveSessionKey(String capacitySessionKey);

    long countOccupiedCapacityUnits(
            Integer sedeId,
            FacilityResourceType resourceType,
            LocalDate appointmentDate,
            LocalTime startTime,
            LocalTime endTime,
            UUID excludeAppointmentId
    );

    AppointmentResourceAllocation save(AppointmentResourceAllocation allocation);
}
