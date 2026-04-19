package com.ipscentir.appointments.application.dto.integration.n8n;

import com.ipscentir.appointments.application.dto.AppointmentDTO;

public record N8nCancelAppointmentResponse(
        AppointmentDTO appointment,
        String summary
) {}