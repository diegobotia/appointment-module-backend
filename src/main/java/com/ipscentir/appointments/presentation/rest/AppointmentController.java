package com.ipscentir.appointments.presentation.rest;

import com.ipscentir.appointments.application.dto.AppointmentDTO;
import com.ipscentir.appointments.application.dto.AppointmentPageResponse;
import com.ipscentir.appointments.application.dto.AppointmentSearchCriteria;
import com.ipscentir.appointments.application.dto.CancelAppointmentCommand;
import com.ipscentir.appointments.application.dto.CreateAdministrativeAppointmentCommand;
import com.ipscentir.appointments.application.dto.CreateAppointmentCommand;
import com.ipscentir.appointments.application.dto.RescheduleAppointmentCommand;
import com.ipscentir.appointments.application.service.AppointmentOperationsService;
import com.ipscentir.appointments.domain.model.appointment.AppointmentStatus;
import com.ipscentir.appointments.domain.model.appointment.BookingChannel;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/appointments")
@RequiredArgsConstructor
@Tag(name = "Appointments API", description = "Gestión operativa de citas para personal interno")
public class AppointmentController {

    private final AppointmentOperationsService appointmentOperationsService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRACION', 'ADMISIONES', 'ASESOR', 'MEDICO', 'FACTURACION')")
    @Operation(
            summary = "Listar citas con filtros y paginación",
            description = "Devuelve AppointmentPageResponse con AppointmentDTO enriquecido (medicoDisplayName, patientDisplayName, administrative). "
                    + "No es necesario resolver IDs de médico o paciente en el cliente."
    )
    public ResponseEntity<AppointmentPageResponse> listAppointments(
            @RequestParam(required = false) Integer sedeId,
            @RequestParam(required = false) String medicoId,
            @RequestParam(required = false, name = "doctorId") String legacyDoctorId,
            @RequestParam(required = false) UUID patientId,
            @RequestParam(required = false) AppointmentStatus status,
            @RequestParam(required = false) BookingChannel bookingChannel,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        String resolvedMedicoId = medicoId != null && !medicoId.isBlank() ? medicoId : legacyDoctorId;
        return ResponseEntity.ok(appointmentOperationsService.searchAppointments(
                new AppointmentSearchCriteria(sedeId, resolvedMedicoId, patientId, status, bookingChannel, fromDate, toDate),
                page, size
        ));
    }

    @GetMapping("/{appointmentId}")
    @PreAuthorize("hasAnyRole('ADMINISTRACION', 'ADMISIONES', 'ASESOR', 'MEDICO', 'FACTURACION')")
    @Operation(
            summary = "Obtener cita por ID",
            description = "Mismo contrato enriquecido que el listado (display names y flag administrative)."
    )
    public ResponseEntity<AppointmentDTO> getAppointment(@PathVariable UUID appointmentId) {
        return ResponseEntity.ok(appointmentOperationsService.getAppointment(appointmentId));
    }

    @PostMapping("/administrative")
    @PreAuthorize("hasAnyRole('ADMINISTRACION', 'ADMISIONES', 'ASESOR')")
    @Operation(
            summary = "Crear cita administrativa (reunión/bloqueo staff sin paciente)",
            description = "Reserva sala REUNION_STAFF y bloquea disponibilidad de los participantes. "
                    + "patientId=null, appointmentType=STAFF, administrative=true. "
                    + "No admite check-in ni flujo clínico de paciente."
    )
    public ResponseEntity<AppointmentDTO> createAdministrativeAppointment(
            @Valid @RequestBody CreateAdministrativeAppointmentCommand command
    ) {
        AppointmentDTO appointment = appointmentOperationsService.createAdministrativeAppointment(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(appointment);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRACION', 'ADMISIONES', 'ASESOR')")
    @Operation(summary = "Crear cita desde mostrador (staff)")
    public ResponseEntity<AppointmentDTO> createAppointment(@Valid @RequestBody CreateAppointmentCommand command) {
        AppointmentDTO appointment = appointmentOperationsService.createAppointment(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(appointment);
    }

    @PatchMapping("/{appointmentId}/confirm")
    @PreAuthorize("hasAnyRole('ADMINISTRACION', 'ADMISIONES', 'ASESOR')")
    @Operation(summary = "Confirmar cita")
    public ResponseEntity<AppointmentDTO> confirmAppointment(@PathVariable UUID appointmentId) {
        return ResponseEntity.ok(appointmentOperationsService.confirmAppointment(appointmentId));
    }

    @PatchMapping("/{appointmentId}/cancel")
    @PreAuthorize("hasAnyRole('ADMINISTRACION', 'ADMISIONES', 'ASESOR')")
    @Operation(summary = "Cancelar cita")
    public ResponseEntity<AppointmentDTO> cancelAppointment(
            @PathVariable UUID appointmentId,
            @Valid @RequestBody CancelAppointmentCommand command
    ) {
        return ResponseEntity.ok(appointmentOperationsService.cancelAppointment(appointmentId, command));
    }

    @PatchMapping("/{appointmentId}/reschedule")
    @PreAuthorize("hasAnyRole('ADMINISTRACION', 'ADMISIONES', 'ASESOR')")
    @Operation(summary = "Reprogramar cita")
    public ResponseEntity<AppointmentDTO> rescheduleAppointment(
            @PathVariable UUID appointmentId,
            @Valid @RequestBody RescheduleAppointmentCommand command
    ) {
        return ResponseEntity.ok(appointmentOperationsService.rescheduleAppointment(appointmentId, command));
    }

    @PatchMapping("/{appointmentId}/check-in")
    @PreAuthorize("hasAnyRole('ADMINISTRACION', 'ADMISIONES', 'ASESOR', 'MEDICO')")
    @Operation(summary = "Registrar check-in de paciente")
    public ResponseEntity<AppointmentDTO> checkIn(@PathVariable UUID appointmentId) {
        return ResponseEntity.ok(appointmentOperationsService.checkInAppointment(appointmentId));
    }

    @PatchMapping("/{appointmentId}/no-show")
    @PreAuthorize("hasAnyRole('ADMINISTRACION', 'ADMISIONES', 'ASESOR')")
    @Operation(summary = "Marcar cita como no-show")
    public ResponseEntity<AppointmentDTO> markNoShow(@PathVariable UUID appointmentId) {
        return ResponseEntity.ok(appointmentOperationsService.markNoShow(appointmentId));
    }

    @PatchMapping("/{appointmentId}/complete")
    @PreAuthorize("hasAnyRole('ADMINISTRACION', 'ADMISIONES', 'ASESOR', 'MEDICO')")
    @Operation(summary = "Completar cita")
    public ResponseEntity<AppointmentDTO> completeAppointment(@PathVariable UUID appointmentId) {
        return ResponseEntity.ok(appointmentOperationsService.completeAppointment(appointmentId));
    }

    @GetMapping("/therapy/pending-group")
    @PreAuthorize("hasAnyRole('ADMINISTRACION', 'ADMISIONES', 'ASESOR')")
    @Operation(summary = "Listar terapias grupales pendientes de confirmación")
    public ResponseEntity<List<AppointmentDTO>> listPendingGroupTherapy() {
        return ResponseEntity.ok(appointmentOperationsService.listPendingGroupTherapyAppointments());
    }

    @PostMapping("/therapy/pending-group/process-cutoff")
    @PreAuthorize("hasRole('ADMINISTRACION')")
    @Operation(summary = "Ejecutar corte operativo de terapias grupales bajo mínimo")
    public ResponseEntity<Map<String, Integer>> processPendingGroupCutoff() {
        int processed = appointmentOperationsService.processPendingGroupTherapyCutoff();
        return ResponseEntity.ok(Map.of("processedCount", processed));
    }
}
