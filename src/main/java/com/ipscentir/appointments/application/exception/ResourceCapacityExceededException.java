package com.ipscentir.appointments.application.exception;

import com.ipscentir.appointments.domain.model.facility.FacilityResourceType;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public class ResourceCapacityExceededException extends IllegalStateException {

    private final Integer sedeId;
    private final FacilityResourceType resourceType;
    private final LocalDate appointmentDate;
    private final LocalTime startTime;
    private final LocalTime endTime;
    private final int totalUnits;
    private final long occupiedUnits;

    public ResourceCapacityExceededException(
            String message,
            Integer sedeId,
            FacilityResourceType resourceType,
            LocalDate appointmentDate,
            LocalTime startTime,
            LocalTime endTime,
            int totalUnits,
            long occupiedUnits
    ) {
        super(message);
        this.sedeId = sedeId;
        this.resourceType = resourceType;
        this.appointmentDate = appointmentDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.totalUnits = totalUnits;
        this.occupiedUnits = occupiedUnits;
    }

    public Integer sedeId() {
        return sedeId;
    }

    public FacilityResourceType resourceType() {
        return resourceType;
    }

    public LocalDate appointmentDate() {
        return appointmentDate;
    }

    public LocalTime startTime() {
        return startTime;
    }

    public LocalTime endTime() {
        return endTime;
    }

    public int totalUnits() {
        return totalUnits;
    }

    public long occupiedUnits() {
        return occupiedUnits;
    }
}
