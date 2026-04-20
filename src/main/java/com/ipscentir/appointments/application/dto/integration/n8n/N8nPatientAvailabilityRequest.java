package com.ipscentir.appointments.application.dto.integration.n8n;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record N8nPatientAvailabilityRequest(
                String serviceType,
                String specialty,
        @NotNull N8nFacilityId facilityId,
                Integer limit,
                LocalDate fromDate
) {

        public N8nPatientAvailabilityRequest {
                if (fromDate == null) {
                        fromDate = LocalDate.now();
                }
                if (limit == null) {
                        limit = 4;
                }
        }

        public String canonicalServiceType() {
                return (serviceType != null && !serviceType.isBlank()) ? serviceType : specialty;
        }
}