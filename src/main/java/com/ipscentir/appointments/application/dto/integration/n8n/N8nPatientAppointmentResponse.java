package com.ipscentir.appointments.application.dto.integration.n8n;

import com.ipscentir.appointments.application.dto.AppointmentDTO;

public record N8nPatientAppointmentResponse(
        AppointmentDTO appointment,
        String summary
) {}