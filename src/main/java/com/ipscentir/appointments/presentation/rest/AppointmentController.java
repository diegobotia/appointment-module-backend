package com.ipscentir.appointments.presentation.rest;

import com.ipscentir.appointments.application.dto.AppointmentDTO;
import com.ipscentir.appointments.application.dto.CreateAppointmentCommand;
import com.ipscentir.appointments.application.service.AppointmentApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/appointments")
@RequiredArgsConstructor
@Tag(name = "Appointments API", description = "Endpoints for booking and managing medical appointments")
public class AppointmentController {

    private final AppointmentApplicationService appointmentApplicationService;

    @PostMapping
    @Operation(summary = "Book an Appointment", description = "Attempts to secure a booking against the Schedule and validates availability")
    public ResponseEntity<AppointmentDTO> bookAppointment(
            @Valid @RequestBody CreateAppointmentCommand command
    ) {
        AppointmentDTO appointment = appointmentApplicationService.createAppointment(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(appointment);
    }
}
