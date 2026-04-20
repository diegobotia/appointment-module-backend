package com.ipscentir.appointments.application.service;

import com.ipscentir.appointments.application.dto.AppointmentDTO;
import com.ipscentir.appointments.application.dto.CreateAppointmentCommand;
import com.ipscentir.appointments.application.dto.integration.n8n.N8nAvailabilityBookingPayloadDTO;
import com.ipscentir.appointments.application.dto.integration.n8n.N8nAvailabilitySlotDTO;
import com.ipscentir.appointments.application.dto.integration.n8n.N8nCancelAppointmentRequest;
import com.ipscentir.appointments.application.dto.integration.n8n.N8nCancelAppointmentResponse;
import com.ipscentir.appointments.application.dto.integration.n8n.N8nPatientAppointmentRequest;
import com.ipscentir.appointments.application.dto.integration.n8n.N8nPatientAppointmentResponse;
import com.ipscentir.appointments.application.dto.integration.n8n.N8nPatientAvailabilityRequest;
import com.ipscentir.appointments.application.dto.integration.n8n.N8nPatientAvailabilityResponse;
import com.ipscentir.appointments.application.dto.integration.n8n.N8nWebhookEventRequest;
import com.ipscentir.appointments.application.dto.integration.n8n.N8nWebhookEventResponse;
import com.ipscentir.appointments.domain.model.catalog.AppointmentServiceType;
import com.ipscentir.appointments.domain.model.appointment.Appointment;
import com.ipscentir.appointments.domain.model.appointment.AppointmentType;
import com.ipscentir.appointments.domain.model.schedule.AvailableSlotDetail;
import com.ipscentir.appointments.domain.service.AppointmentBookingService;
import com.ipscentir.appointments.domain.service.AvailabilityService;
import com.ipscentir.appointments.application.mapper.AppointmentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class N8nPatientIntegrationService {

    private final AvailabilityService availabilityService;
    private final AppointmentApplicationService appointmentApplicationService;
    private final AppointmentBookingService appointmentBookingService;
    private final AppointmentMapper appointmentMapper;
    private final N8nEventJournalService n8nEventJournalService;

    @Transactional(readOnly = true)
    public N8nPatientAvailabilityResponse getAvailability(N8nPatientAvailabilityRequest request) {
        String requestedServiceType = request.canonicalServiceType();
        AppointmentServiceType serviceType = AppointmentServiceType.fromFlexibleValue(requestedServiceType);

        List<AvailableSlotDetail> slots = availabilityService.getNearestAvailableSlotsByServiceType(
                serviceType,
                request.facilityId(),
                request.fromDate(),
                request.limit()
        );

        List<N8nAvailabilitySlotDTO> responseSlots = slots.stream()
                .map(slot -> new N8nAvailabilitySlotDTO(
                        slot.scheduleId(),
                        slot.doctorId(),
                        slot.facilityId(),
                        slot.serviceType().name(),
                        slot.specialty(),
                        slot.appointmentDate(),
                        slot.appointmentTime(),
                        slot.durationMinutes(),
                        "PRESENCIAL",
                        slot.availableSeats(),
                        new N8nAvailabilityBookingPayloadDTO(
                                null,
                                slot.doctorId(),
                                slot.facilityId(),
                                null,
                                slot.scheduleId(),
                                slot.appointmentDate(),
                                slot.appointmentTime(),
                                "PRESENCIAL",
                                slot.serviceType().name(),
                                slot.specialty(),
                                slot.durationMinutes(),
                                slot.availableSeats(),
                                null
                        )
                ))
                .toList();

        String summary = responseSlots.isEmpty()
                ? "No hay cupos disponibles para la fecha solicitada."
                : "Se encontraron " + responseSlots.size() + " horarios disponibles para " + serviceType.getDisplayName() + ".";

        return new N8nPatientAvailabilityResponse(
                request.facilityId(),
                serviceType.name(),
                serviceType.getDisplayName(),
                request.fromDate(),
                request.limit(),
                responseSlots.size(),
                responseSlots,
                summary
        );
    }

    @Transactional
    public N8nPatientAppointmentResponse createAppointment(N8nPatientAppointmentRequest request) {
        AppointmentDTO appointment = appointmentApplicationService.createAppointment(
                new CreateAppointmentCommand(
                        request.patientId(),
                        request.doctorId(),
                        request.facilityId(),
                        request.secondaryDoctorId(),
                        request.scheduleId(),
                        request.appointmentDate(),
                        request.appointmentTime(),
                        AppointmentType.PRESENCIAL.name(),
                        request.reason()
                )
        );

        return new N8nPatientAppointmentResponse(
                appointment,
                "Cita agendada correctamente para atención por chat n8n."
        );
    }

    @Transactional
    public N8nCancelAppointmentResponse cancelAppointment(java.util.UUID appointmentId, N8nCancelAppointmentRequest request) {
        Appointment cancelled = appointmentBookingService.cancelAppointment(appointmentId, request.reason());
        AppointmentDTO dto = appointmentMapper.toDto(cancelled);

        return new N8nCancelAppointmentResponse(
                dto,
                "Cita cancelada correctamente desde el flujo n8n."
        );
    }

    @Transactional
    public N8nWebhookEventResponse handleWebhookEvent(N8nWebhookEventRequest request) {
        return n8nEventJournalService.handleWebhookEvent(request);
    }
}