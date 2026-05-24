package com.ipscentir.appointments.application.service;

import com.ipscentir.appointments.application.dto.AppointmentDTO;
import com.ipscentir.appointments.application.dto.CreateAppointmentCommand;
import com.ipscentir.appointments.application.dto.RescheduleAppointmentCommand;
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
import com.ipscentir.appointments.application.dto.integration.n8n.N8nRescheduleAppointmentRequest;
import com.ipscentir.appointments.application.dto.integration.n8n.N8nConfirmAppointmentRequest;
import com.ipscentir.appointments.application.dto.integration.n8n.N8nConfirmAppointmentResponse;
import com.ipscentir.appointments.application.dto.integration.n8n.N8nPendingRemindersResponse;
import com.ipscentir.appointments.application.dto.integration.n8n.N8nWebhookEventRequest;
import com.ipscentir.appointments.application.dto.integration.n8n.N8nWebhookEventResponse;
import com.ipscentir.appointments.application.exception.SedeNotFoundException;
import com.ipscentir.appointments.application.exception.PatientNotFoundException;
import com.ipscentir.appointments.application.mapper.AppointmentMapper;
import com.ipscentir.appointments.domain.model.appointment.Appointment;
import com.ipscentir.appointments.domain.model.appointment.AppointmentStatus;
import com.ipscentir.appointments.domain.model.appointment.AppointmentType;
import com.ipscentir.appointments.domain.model.appointment.BookingChannel;
import com.ipscentir.appointments.domain.model.catalog.AppointmentServiceType;
import com.ipscentir.appointments.domain.model.sede.Sede;
import com.ipscentir.appointments.domain.model.specialist.Specialist;
import com.ipscentir.appointments.domain.model.schedule.AvailableSlotDetail;
import com.ipscentir.appointments.domain.model.schedule.Schedule;
import com.ipscentir.appointments.domain.repository.AppointmentRepository;
import com.ipscentir.appointments.domain.repository.ScheduleRepository;
import com.ipscentir.appointments.domain.service.AppointmentBookingService;
import com.ipscentir.appointments.domain.service.AvailabilityService;
import com.ipscentir.appointments.infrastructure.persistence.jpa.PacienteRepository;
import com.ipscentir.appointments.infrastructure.persistence.jpa.SpecialistJpaRepository;
import com.ipscentir.appointments.infrastructure.persistence.jpa.entity.Paciente;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
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
    private final SedeLookupService sedeLookupService;
    private final ScheduleRepository scheduleRepository;
    private final SpecialistJpaRepository specialistJpaRepository;
    private final N8nIdempotencyService n8nIdempotencyService;
    private final AppointmentOperationsService appointmentOperationsService;
    private final TipoIdentificacionResolver tipoIdentificacionResolver;

    @Transactional(readOnly = true)
    public N8nPatientAvailabilityResponse getAvailability(N8nPatientAvailabilityRequest request) {
        String requestedServiceType = request.canonicalServiceType();
        AppointmentServiceType serviceType = AppointmentServiceType.fromFlexibleValue(requestedServiceType);
        Integer resolvedSedeId = resolveSedeId(request.facilityId());

        List<AvailableSlotDetail> slots = availabilityService.getNearestAvailableSlotsByServiceType(
                serviceType,
                resolvedSedeId,
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

        Integer resolvedSedeId = resolveSedeId(request.facilityId());

        AppointmentDTO appointment = appointmentApplicationService.createAppointment(
                new CreateAppointmentCommand(
                        request.patientId(),
                        request.doctorId(),
                        resolvedSedeId,
                        request.secondaryDoctorId(),
                        request.scheduleId(),
                        request.appointmentDate(),
                        request.appointmentTime(),
                        AppointmentType.PRESENCIAL.name(),
                        request.reason(),
                        BookingChannel.N8N,
                        request.conversationId()
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
        return tipoIdentificacionResolver
                .findPaciente(request.codTipoIdentificacion(), request.numIdentificacion())
                .map(this::toIdentifyResponse)
                .orElseGet(() -> toIdentifyNotFound(request.codTipoIdentificacion(), request.numIdentificacion()));
    }

    @Transactional
    public PatientRegistrationResponse registerPatient(CreatePatientRegistrationRequest request) {
        return patientRegistrationService.registerPatient(request);
    }

    @Transactional(readOnly = true)
    public N8nPatientAppointmentsResponse listAppointmentsByDocument(String codTipoIdentificacion, String numIdentificacion) {
        Paciente paciente = tipoIdentificacionResolver
                .findPaciente(codTipoIdentificacion, numIdentificacion)
                .orElseThrow(() -> new PatientNotFoundException(
                        tipoIdentificacionResolver.resolveCodigo(codTipoIdentificacion),
                        numIdentificacion
                ));

        List<N8nPatientAppointmentSummaryDTO> appointments = appointmentRepository
                .findByPatientIdOrderByAppointmentDateDescAppointmentTimeDesc(paciente.getId())
                .stream()
                .map(this::toAppointmentSummary)
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
        String codigo = tipoIdentificacionResolver.resolveCodigo(codTipoIdentificacion);
        PatientRegistrationStatusResponse status = tipoIdentificacionResolver
                .findPaciente(codTipoIdentificacion, numIdentificacion)
                .map(paciente -> new PatientRegistrationStatusResponse(
                        true,
                        paciente.getId(),
                        paciente.getCodTipoIdentificacion(),
                        paciente.getNumIdentificacion(),
                        paciente.getNombres(),
                        paciente.getApellidos()
                ))
                .orElseGet(() -> PatientRegistrationStatusResponse.notRegistered(codigo, numIdentificacion));
        PatientRegistrationFormConfigResponse config = patientRegistrationService.getFormConfig();
        String formUrl = buildFormUrl(config, codigo, numIdentificacion);

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
    public N8nPatientAppointmentResponse rescheduleAppointment(UUID appointmentId, N8nRescheduleAppointmentRequest request) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found"));

        if (request.patientId() != null && !request.patientId().equals(appointment.getPatientId())) {
            throw new IllegalStateException("La cita no pertenece al paciente indicado");
        }

        String idempotencyKey = request.resolveIdempotencyKey();
        if (idempotencyKey != null) {
            var existingId = n8nIdempotencyService.findAppointmentId(
                    N8nIdempotencyService.SCOPE_RESCHEDULE_APPOINTMENT,
                    idempotencyKey
            );
            if (existingId.isPresent() && existingId.get().equals(appointmentId)) {
                Appointment existing = appointmentRepository.findById(appointmentId).orElseThrow();
                return new N8nPatientAppointmentResponse(
                        appointmentMapper.toDto(existing),
                        "Cita ya reprogramada previamente para esta conversación (idempotencia n8n)."
                );
            }
        }

        Integer resolvedSedeId = resolveSedeId(request.facilityId());
        AppointmentDTO dto = appointmentOperationsService.rescheduleAppointmentFromN8n(
                appointmentId,
                new RescheduleAppointmentCommand(
                        request.appointmentDate(),
                        request.appointmentTime(),
                        request.scheduleId(),
                        request.doctorId(),
                        resolvedSedeId
                ),
                request.conversationId()
        );

        if (idempotencyKey != null) {
            n8nIdempotencyService.storeAppointmentBooking(
                    N8nIdempotencyService.SCOPE_RESCHEDULE_APPOINTMENT,
                    idempotencyKey,
                    appointmentId
            );
        }

        return new N8nPatientAppointmentResponse(dto, "Cita reprogramada correctamente desde el flujo n8n.");
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

    @Transactional(readOnly = true)
    public N8nPendingRemindersResponse getPendingReminders(LocalDate date) {
        LocalDate targetDate = date != null ? date : LocalDate.now().plusDays(1);
        
        List<N8nPatientAppointmentSummaryDTO> appointments = appointmentRepository
                .findByDateAndStatusIn(targetDate, List.of(AppointmentStatus.SCHEDULED))
                .stream()
                .map(this::toAppointmentSummary)
                .toList();

        String summary = appointments.isEmpty()
                ? "No hay recordatorios pendientes."
                : "Se encontraron " + appointments.size() + " recordatorios pendientes para la fecha " + targetDate + ".";

        return new N8nPendingRemindersResponse(
                targetDate,
                appointments.size(),
                appointments,
                summary
        );
    }

    @Transactional
    public N8nConfirmAppointmentResponse confirmAppointment(UUID appointmentId, N8nConfirmAppointmentRequest request) {
        AppointmentDTO dto = appointmentOperationsService.confirmAppointment(appointmentId);

        return new N8nConfirmAppointmentResponse(
                dto,
                "Cita confirmada correctamente desde el flujo n8n. " + (request.reason() != null ? request.reason() : "")
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
        String codigo = tipoIdentificacionResolver.resolveCodigo(paciente.getCodTipoIdentificacion());
        return new N8nPatientIdentifyResponse(
                true,
                paciente.getId(),
                tipoIdentificacionResolver.resolveDescripcion(paciente.getCodTipoIdentificacion()),
                paciente.getNumIdentificacion(),
                paciente.getNombres(),
                paciente.getApellidos(),
                buildFormUrl(config, codigo, paciente.getNumIdentificacion()),
                "Paciente identificado: " + paciente.getNombres() + " " + paciente.getApellidos()
        );
    }

    private N8nPatientIdentifyResponse toIdentifyNotFound(String tipoIdentificacion, String numIdentificacion) {
        PatientRegistrationFormConfigResponse config = patientRegistrationService.getFormConfig();
        String codigo = tipoIdentificacionResolver.resolveCodigo(tipoIdentificacion);
        return new N8nPatientIdentifyResponse(
                false,
                null,
                tipoIdentificacionResolver.resolveDescripcion(tipoIdentificacion),
                numIdentificacion,
                null,
                null,
                buildFormUrl(config, codigo, numIdentificacion),
                "Paciente no encontrado. Debe completar el formulario de registro."
        );
    }

    private String buildFormUrl(PatientRegistrationFormConfigResponse config, String codTipo, String numId) {
        return config.urlTemplate()
                .replace("{codTipoIdentificacion}", codTipo)
                .replace("{numIdentificacion}", numId);
    }

    private Integer resolveSedeId(N8nFacilityId facilityId) {
        return facilityId.sedeId();
    }

    private N8nPatientAppointmentSummaryDTO toAppointmentSummary(Appointment appointment) {
        Schedule schedule = appointment.getScheduleId() != null
                ? scheduleRepository.findById(appointment.getScheduleId()).orElse(null)
                : null;
        Optional<AppointmentServiceType> catalogService = resolveCatalogService(
                schedule,
                appointment.getDoctorId(),
                appointment.getAppointmentType()
        );

        return new N8nPatientAppointmentSummaryDTO(
                appointment.getId(),
                appointment.getPatientId(),
                appointment.getDoctorId(),
                appointment.getSedeId(),
                resolveN8nFacilityCode(appointment.getSedeId()),
                appointment.getScheduleId(),
                appointment.getAppointmentDate(),
                appointment.getAppointmentTime(),
                appointment.getAppointmentType(),
                catalogService.map(Enum::name).orElse(null),
                catalogService.map(AppointmentServiceType::getDisplayName).orElse(null),
                appointment.getStatus(),
                appointment.getReason()
        );
    }

    /**
     * Catálogo cerrado: serviceType (código) y specialty (nombre para UI) salen del mismo
     * {@link AppointmentServiceType}. Si no hay match, ambos quedan null.
     *
     * Fuentes, en orden: agenda → hc.medicos.especialidad → tipo de cita (terapia/junta).
     */
    private Optional<AppointmentServiceType> resolveCatalogService(
            Schedule schedule,
            String doctorId,
            AppointmentType appointmentType
    ) {
        if (schedule != null && schedule.getSpecialty() != null && !schedule.getSpecialty().isBlank()) {
            Optional<AppointmentServiceType> fromSchedule = AppointmentServiceType.tryResolve(schedule.getSpecialty());
            if (fromSchedule.isPresent()) {
                return fromSchedule;
            }
        }

        if (doctorId != null) {
            Optional<AppointmentServiceType> fromDoctor = specialistJpaRepository.findById(doctorId)
                    .map(Specialist::getSpecialty)
                    .flatMap(AppointmentServiceType::tryResolve);
            if (fromDoctor.isPresent()) {
                return fromDoctor;
            }
        }

        return switch (appointmentType) {
            case TERAPIA_FISICA -> Optional.of(AppointmentServiceType.TERAPIA_FISICA);
            case TERAPIA_OCUPACIONAL -> Optional.of(AppointmentServiceType.TERAPIA_OCUPACIONAL);
            case JUNTA_MEDICA -> Optional.of(AppointmentServiceType.JUNTA_MEDICA);
            case STAFF -> Optional.of(AppointmentServiceType.STAFF);
            case PRESENCIAL -> Optional.empty();
        };
    }

    private String resolveN8nFacilityCode(Integer sedeId) {
        if (sedeId == null) {
            return null;
        }
        return Arrays.stream(N8nFacilityId.values())
                .filter(n8n -> n8n.sedeId() == sedeId)
                .findFirst()
                .map(Enum::name)
                .orElse(null);
    }
}
