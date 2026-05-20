package com.ipscentir.appointments.presentation.rest;

import com.ipscentir.appointments.application.dto.AppointmentDTO;
import com.ipscentir.appointments.application.dto.AppointmentSearchCriteria;
import com.ipscentir.appointments.application.service.AppointmentOperationsService;
import com.ipscentir.appointments.domain.model.appointment.AppointmentStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/appointments")
@PreAuthorize("hasAnyRole('ADMINISTRACION', 'ADMISIONES')")
@RequiredArgsConstructor
@Tag(name = "Admin Appointments API", description = "Operación administrativa de citas")
public class AdminAppointmentController {

    private final AppointmentOperationsService appointmentOperationsService;

    @GetMapping
    @Operation(summary = "Buscar citas (mismos filtros que operación staff)")
    public ResponseEntity<List<AppointmentDTO>> search(
            @RequestParam(required = false) UUID facilityId,
            @RequestParam(required = false) String doctorId,
            @RequestParam(required = false) UUID patientId,
            @RequestParam(required = false) AppointmentStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
    ) {
        return ResponseEntity.ok(appointmentOperationsService.searchAppointments(
                new AppointmentSearchCriteria(facilityId, doctorId, patientId, status, fromDate, toDate)
        ));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalle de cita")
    public ResponseEntity<AppointmentDTO> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(appointmentOperationsService.getAppointment(id));
    }
}
