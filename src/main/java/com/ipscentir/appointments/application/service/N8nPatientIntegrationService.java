package com.ipscentir.appointments.application.service;

import com.ipscentir.appointments.application.dto.AppointmentDTO;
import com.ipscentir.appointments.application.dto.CreateAppointmentCommand;
import com.ipscentir.appointments.application.dto.form.CreatePatientRegistrationRequest;
import com.ipscentir.appointments.application.dto.form.PatientRegistrationFormConfigResponse;
import com.ipscentir.appointments.application.dto.form.PatientRegistrationResponse;
import com.ipscentir.appointments.application.dto.form.PatientRegistrationStatusResponse;
import com.ipscentir.appointments.application.dto.integration.n8n.N8nAvailabilityBookingPayloadDTO;
import com.ipscentir.appointments.application.dto.integration.n8n.N8nAvailabilitySlotDTO;
import com.ipscentir.appointments.application.dto.integration.n8n.N8nCancelAppointmentRequest;
import com.ipscentir.appointments.application.dto.integration.n8n.N8nCancelAppointmentResponse;
import com.ipscentir.appointments.application.dto.integration.n8n.N8nFacilityId;
import com.ipscentir.appointments.application.dto.integration.n8n.N8nPatientAppointmentRequest;
import com.ipscentir.appointments.application.dto.integration.n8n.N8nPatientAppointmentResponse;
import com.ipscentir.appointments.application.dto.integration.n8n.N8nPatientAppointmentSummaryDTO;
import com.ipscentir.appointments.application.dto.integration.n8n.N8nPatientAppointmentsResponse;
import com.ipscentir.appointments.application.dto.integration.n8n.N8nPatientAvailabilityRequest;
import com.ipscentir.appointments.application.dto.integration.n8n.N8nPatientAvailabilityResponse;
import com.ipscentir.appointments.application.dto.integration.n8n.N8nPatientIdentifyRequest;
import com.ipscentir.appointments.application.dto.integration.n8n.N8nPatientIdentifyResponse;
import com.ipscentir.appointments.application.dto.integration.n8n.N8nPatientRegistrationStatusResponse;
import com.ipscentir.appointments.application.dto.integration.n8n.N8nWebhookEventRequest;
import com.ipscentir.appointments.application.dto.integration.n8n.N8nWebhookEventResponse;
import com.ipscentir.appointments.application.exception.PatientNotFoundException;
import com.ipscentir.appointments.application.mapper.AppointmentMapper;
import com.ipscentir.appointments.domain.model.appointment.Appointment;
import com.ipscentir.appointments.domain.model.appointment.AppointmentType;
import com.ipscentir.appointments.domain.model.catalog.AppointmentServiceType;
import com.ipscentir.appointments.domain.model.facility.Facility;
import com.ipscentir.appointments.domain.model.schedule.AvailableSlotDetail;
import com.ipscentir.appointments.domain.repository.AppointmentRepository;
import com.ipscentir.appointments.domain.service.AppointmentBookingService;
import com.ipscentir.appointments.domain.service.AvailabilityService;
import com.ipscentir.appointments.infrastructure.persistence.jpa.FacilityJpaRepository;
import com.ipscentir.appointments.infrastructure.persistence.jpa.PacienteRepository;
import com.ipscentir.appointments.infrastructure.persistence.jpa.entity.Paciente;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class N8nPatientIntegrationService {

    private final AvailabilityService availabilityService;
    private final AppointmentApplicationService appointmentApplicationService;
    private final AppointmentBookingService appointmentBookingService;
    private final AppointmentMapper appointmentMapper;
    private final AppointmentRepository appointmentRepository;
    private final N8nEventJournalService n8nEventJournalService;
    private final PatientRegistrationService patientRegistrationService;
    private final PacienteRepository pacienteRepository;
    private final FacilityJpaRepository facilityJpaRepository;
    private final N8nIdempotencyService n8nIdempotencyService;

    @Transactional(readOnly = true)
    public N8nPatientAvailabilityResponse getAvailability(N8nPatientAvailabilityRequest request) {
        String requestedServiceType = request.canonicalServiceType();
        AppointmentServiceType serviceType = AppointmentServiceType.fromFlexibleValue(requestedServiceType);
        UUID resolvedFacilityId = resolveFacilityId(request.facilityId());

        List<AvailableSlotDetail> slots = availabilityService.getNearestAvailableSlotsByServiceType(
                serviceType,
                resolvedFacilityId,
                request.fromDate(),
                request.limit()
        );

        List<N8nAvailabilitySlotDTO> responseSlots = slots.stream()
                .map(slot -> new N8nAvailabilitySlotDTO(
                        slot.scheduleId(),
                        slot.doctorId(),
                        request.facilityId(),
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
                                request.facilityId(),
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
                serviceType.name(),
                request.fromDate(),
                request.limit(),
                responseSlots.size(),
                responseSlots,
                summary
        );
    }

    @Transactional
    public N8nPatientAppointmentResponse createAppointment(N8nPatientAppointmentRequest request) {
        requirePatientExists(request.patientId());

        String idempotencyKey = request.resolveIdempotencyKey();
        if (idempotencyKey != null) {
            var existingAppointmentId = n8nIdempotencyService.findAppointmentId(
                    N8nIdempotencyService.SCOPE_BOOK_APPOINTMENT,
                    idempotencyKey
            );
            if (existingAppointmentId.isPresent()) {
                Appointment existing = appointmentRepository.findById(existingAppointmentId.get())
                        .orElseThrow(() -> new IllegalStateException("Idempotent appointment reference not found"));
                return new N8nPatientAppointmentResponse(
                        appointmentMapper.toDto(existing),
                        "Cita ya registrada previamente para esta conversación (idempotencia n8n)."
                );
            }
        }

        UUID resolvedFacilityId = resolveFacilityId(request.facilityId());

        AppointmentDTO appointment = appointmentApplicationService.createAppointment(
                new CreateAppointmentCommand(
                        request.patientId(),
                        request.doctorId(),
                        resolvedFacilityId,
                        request.secondaryDoctorId(),
                        request.scheduleId(),
                        request.appointmentDate(),
                        request.appointmentTime(),
                        AppointmentType.PRESENCIAL.name(),
                        request.reason()
                )
        );

        if (idempotencyKey != null) {
            n8nIdempotencyService.storeAppointmentBooking(idempotencyKey, appointment.id());
        }

        return new N8nPatientAppointmentResponse(
                appointment,
                "Cita agendada correctamente para atención por chat n8n."
        );
    }

    @Transactional(readOnly = true)
    public N8nPatientIdentifyResponse identifyPatient(N8nPatientIdentifyRequest request) {
        return pacienteRepository
                .findByCodTipoIdentificacionAndNumIdentificacion(
                        request.codTipoIdentificacion(),
                        request.numIdentificacion()
                )
                .map(this::toIdentifyResponse)
                .orElseGet(() -> toIdentifyNotFound(request.codTipoIdentificacion(), request.numIdentificacion()));
    }

    @Transactional
    public PatientRegistrationResponse registerPatient(CreatePatientRegistrationRequest request) {
        return patientRegistrationService.registerPatient(request);
    }

    @Transactional(readOnly = true)
    public N8nPatientAppointmentsResponse listAppointmentsByDocument(String codTipoIdentificacion, String numIdentificacion) {
        Paciente paciente = pacienteRepository
                .findByCodTipoIdentificacionAndNumIdentificacion(codTipoIdentificacion, numIdentificacion)
                .orElseThrow(() -> new PatientNotFoundException(codTipoIdentificacion, numIdentificacion));

        List<N8nPatientAppointmentSummaryDTO> appointments = appointmentRepository
                .findByPatientIdOrderByAppointmentDateDescAppointmentTimeDesc(paciente.getId())
                .stream()
                .map(appointment -> new N8nPatientAppointmentSummaryDTO(
                        appointment.getId(),
                        appointment.getPatientId(),
                        appointment.getDoctorId(),
                        appointment.getFacilityId(),
                        appointment.getAppointmentDate(),
                        appointment.getAppointmentTime(),
                        appointment.getAppointmentType(),
                        appointment.getStatus(),
                        appointment.getReason()
                ))
                .toList();

        String summary = appointments.isEmpty()
                ? "El paciente no tiene citas registradas."
                : "Se encontraron " + appointments.size() + " citas para el paciente.";

        return new N8nPatientAppointmentsResponse(
                paciente.getId(),
                paciente.getCodTipoIdentificacion(),
                paciente.getNumIdentificacion(),
                appointments.size(),
                appointments,
                summary
        );
    }

    public N8nPatientRegistrationStatusResponse getPatientRegistrationStatus(
            String codTipoIdentificacion,
            String numIdentificacion
    ) {
        PatientRegistrationStatusResponse status = patientRegistrationService.getRegistrationStatus(
                codTipoIdentificacion,
                numIdentificacion
        );
        PatientRegistrationFormConfigResponse config = patientRegistrationService.getFormConfig();
        String formUrl = buildFormUrl(config, codTipoIdentificacion, numIdentificacion);

        String summary = status.registered()
                ? "Paciente registrado: " + status.nombres() + " " + status.apellidos()
                : "Paciente no registrado. Comparta el formulario para completar el registro.";

        return new N8nPatientRegistrationStatusResponse(
                status.registered(),
                status.patientId(),
                status.codTipoIdentificacion(),
                status.numIdentificacion(),
                status.nombres(),
                status.apellidos(),
                formUrl,
                summary
        );
    }

    @Transactional
    public N8nCancelAppointmentResponse cancelAppointment(UUID appointmentId, N8nCancelAppointmentRequest request) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found"));

        if (request.patientId() != null && !request.patientId().equals(appointment.getPatientId())) {
            throw new IllegalStateException("La cita no pertenece al paciente indicado");
        }

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

    private void requirePatientExists(UUID patientId) {
        if (!pacienteRepository.existsById(patientId)) {
            throw new PatientNotFoundException(patientId);
        }
    }

    private N8nPatientIdentifyResponse toIdentifyResponse(Paciente paciente) {
        PatientRegistrationFormConfigResponse config = patientRegistrationService.getFormConfig();
        return new N8nPatientIdentifyResponse(
                true,
                paciente.getId(),
                paciente.getCodTipoIdentificacion(),
                paciente.getNumIdentificacion(),
                paciente.getNombres(),
                paciente.getApellidos(),
                buildFormUrl(config, paciente.getCodTipoIdentificacion(), paciente.getNumIdentificacion()),
                "Paciente identificado: " + paciente.getNombres() + " " + paciente.getApellidos()
        );
    }

    private N8nPatientIdentifyResponse toIdentifyNotFound(String codTipoIdentificacion, String numIdentificacion) {
        PatientRegistrationFormConfigResponse config = patientRegistrationService.getFormConfig();
        return new N8nPatientIdentifyResponse(
                false,
                null,
                codTipoIdentificacion,
                numIdentificacion,
                null,
                null,
                buildFormUrl(config, codTipoIdentificacion, numIdentificacion),
                "Paciente no encontrado. Debe completar el formulario de registro."
        );
    }

    private String buildFormUrl(PatientRegistrationFormConfigResponse config, String codTipo, String numId) {
        return config.urlTemplate()
                .replace("{codTipoIdentificacion}", codTipo)
                .replace("{numIdentificacion}", numId);
    }

    private UUID resolveFacilityId(N8nFacilityId facilityId) {
        return facilityJpaRepository.findByCode(facilityId.persistenceCode())
                .map(Facility::getId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown facilityId: " + facilityId));
    }
}
