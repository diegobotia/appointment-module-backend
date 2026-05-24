package com.ipscentir.appointments.application.service;

import com.ipscentir.appointments.application.dto.AppointmentDTO;
import com.ipscentir.appointments.application.dto.CreateAdministrativeAppointmentCommand;
import com.ipscentir.appointments.application.dto.CreateAppointmentCommand;
import com.ipscentir.appointments.application.security.SedeAuthorizationService;
import com.ipscentir.appointments.domain.model.appointment.Appointment;
import com.ipscentir.appointments.domain.model.appointment.AppointmentType;
import com.ipscentir.appointments.domain.service.AdministrativeAppointmentBookingRequest;
import com.ipscentir.appointments.domain.service.AppointmentBookingRequest;
import com.ipscentir.appointments.domain.service.AppointmentBookingService;
import com.ipscentir.appointments.infrastructure.observability.AppointmentsMetrics;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AppointmentApplicationService {

    private final AppointmentBookingService appointmentBookingService;
    private final AppointmentEnrichmentService appointmentEnrichmentService;
    private final SedeAuthorizationService sedeAuthorizationService;
    private final AppointmentsMetrics appointmentsMetrics;

    public AppointmentDTO createAppointment(CreateAppointmentCommand command) {
        sedeAuthorizationService.assertCurrentUserCanAccessSede(command.sedeId());
        
        // El Application Service recibe el Input DTO (Command), extrae los parámetros,
        // maneja la transacción orquestando el Core Domain (Booking Service),
        // y retorna un Output DTO.
        
        Appointment appointment = appointmentBookingService.bookAppointment(
                new AppointmentBookingRequest(
                        command.patientId(),
                        command.medicoId(),
                        command.secondaryMedicoId(),
                        command.scheduleId(),
                        command.sedeId(),
                        command.appointmentDate(),
                        command.appointmentTime(),
                        AppointmentType.valueOf(command.appointmentType().toUpperCase()),
                        command.reason(),
                        command.resolvedChannel(),
                        command.n8nConversationId()
                )
        );

        appointmentsMetrics.recordAppointmentCreated(appointment.getBookingChannel());
        return appointmentEnrichmentService.toDto(appointment);
    }

    public AppointmentDTO createAdministrativeAppointment(CreateAdministrativeAppointmentCommand command) {
        sedeAuthorizationService.assertCurrentUserCanAccessSede(command.sedeId());
        assertDistinctParticipants(command.participantMedicoIds());

        Appointment appointment = appointmentBookingService.bookAdministrativeAppointment(
                new AdministrativeAppointmentBookingRequest(
                        List.copyOf(command.participantMedicoIds()),
                        command.sedeId(),
                        command.appointmentDate(),
                        command.appointmentTime(),
                        command.resolvedDurationMinutes(),
                        command.reason()
                )
        );

        appointmentsMetrics.recordAppointmentCreated(appointment.getBookingChannel());
        return appointmentEnrichmentService.toDto(appointment);
    }

    private static void assertDistinctParticipants(List<String> participantMedicoIds) {
        if (new HashSet<>(participantMedicoIds).size() != participantMedicoIds.size()) {
            throw new IllegalArgumentException("Los participantes no pueden repetirse");
        }
    }
}
