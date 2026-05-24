package com.ipscentir.appointments.application.service;

import com.ipscentir.appointments.application.dto.AppointmentDTO;
import com.ipscentir.appointments.application.dto.AppointmentSearchCriteria;
import com.ipscentir.appointments.application.dto.CancelAppointmentCommand;
import com.ipscentir.appointments.application.dto.CreateAdministrativeAppointmentCommand;
import com.ipscentir.appointments.application.dto.CreateAppointmentCommand;
import com.ipscentir.appointments.application.dto.RescheduleAppointmentCommand;
import com.ipscentir.appointments.application.security.SedeAuthorizationService;
import com.ipscentir.appointments.application.security.StaffSecurityHelper;
import com.ipscentir.appointments.domain.model.appointment.Appointment;
import com.ipscentir.appointments.domain.model.appointment.AppointmentStatus;
import com.ipscentir.appointments.domain.model.appointment.AppointmentType;
import com.ipscentir.appointments.domain.model.appointment.BookingChannel;
import com.ipscentir.appointments.domain.model.security.RoleName;
import com.ipscentir.appointments.domain.repository.AppointmentRepository;
import com.ipscentir.appointments.domain.service.AppointmentBookingService;
import com.ipscentir.appointments.domain.service.HumanResourceAvailabilityService;
import com.ipscentir.appointments.domain.service.HumanResourceBookingContext;
import com.ipscentir.appointments.domain.service.ResourceCapacityService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AppointmentOperationsService {

    private final AppointmentRepository appointmentRepository;
    private final AppointmentApplicationService appointmentApplicationService;
    private final AppointmentBookingService appointmentBookingService;
    private final AppointmentEnrichmentService appointmentEnrichmentService;
    private final SedeAuthorizationService sedeAuthorizationService;
    private final StaffSecurityHelper staffSecurityHelper;
    private final TherapyPendingGroupCutoffService therapyPendingGroupCutoffService;
    private final HumanResourceAvailabilityService humanResourceAvailabilityService;
    private final ResourceCapacityService resourceCapacityService;

    @Transactional(readOnly = true)
    public List<AppointmentDTO> searchAppointments(AppointmentSearchCriteria criteria) {
        AppointmentSearchCriteria effective = applyRoleFilters(criteria);
        List<Appointment> appointments = appointmentRepository.search(toFilter(effective)).stream()
                .filter(this::canRead)
                .toList();
        return appointmentEnrichmentService.toDtos(appointments);
    }

    @Transactional(readOnly = true)
    public AppointmentDTO getAppointment(UUID appointmentId) {
        Appointment appointment = requireReadableAppointment(appointmentId);
        return appointmentEnrichmentService.toDto(appointment);
    }

    @Transactional
    public AppointmentDTO createAppointment(CreateAppointmentCommand command) {
        requireWriteRole();
        sedeAuthorizationService.assertCurrentUserCanAccessSede(command.sedeId());
        CreateAppointmentCommand staffCommand = new CreateAppointmentCommand(
                command.patientId(),
                command.medicoId(),
                command.sedeId(),
                command.secondaryMedicoId(),
                command.scheduleId(),
                command.appointmentDate(),
                command.appointmentTime(),
                command.appointmentType(),
                command.reason(),
                BookingChannel.STAFF,
                null
        );
        return appointmentApplicationService.createAppointment(staffCommand);
    }

    @Transactional
    public AppointmentDTO createAdministrativeAppointment(CreateAdministrativeAppointmentCommand command) {
        requireWriteRole();
        return appointmentApplicationService.createAdministrativeAppointment(command);
    }

    @Transactional
    public AppointmentDTO confirmAppointment(UUID appointmentId) {
        Appointment appointment = requireWritableAppointment(appointmentId);
        appointment.confirm();
        return appointmentEnrichmentService.toDto(appointmentRepository.save(appointment));
    }

    @Transactional
    public AppointmentDTO cancelAppointment(UUID appointmentId, CancelAppointmentCommand command) {
        Appointment appointment = requireWritableAppointment(appointmentId);
        sedeAuthorizationService.assertCurrentUserCanAccessSede(appointment.getSedeId());
        Appointment cancelled = appointmentBookingService.cancelAppointment(appointmentId, command.reason());
        return appointmentEnrichmentService.toDto(cancelled);
    }

    @Transactional
    public AppointmentDTO rescheduleAppointment(UUID appointmentId, RescheduleAppointmentCommand command) {
        Appointment appointment = requireWritableAppointment(appointmentId);
        sedeAuthorizationService.assertCurrentUserCanAccessSede(command.sedeId());
        sedeAuthorizationService.assertCurrentUserCanAccessSede(appointment.getSedeId());

        rescheduleWithHumanAndPhysicalValidation(appointment, appointmentId, command, BookingChannel.STAFF, null);
        return appointmentEnrichmentService.toDto(appointmentRepository.findById(appointmentId).orElseThrow());
    }

    /**
     * Reprogramación sin validación de rol JWT (flujo n8n con API key).
     */
    @Transactional
    public AppointmentDTO rescheduleAppointmentFromN8n(
            UUID appointmentId,
            RescheduleAppointmentCommand command,
            String n8nConversationId
    ) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found"));

        rescheduleWithHumanAndPhysicalValidation(appointment, appointmentId, command, BookingChannel.N8N, n8nConversationId);
        return appointmentEnrichmentService.toDto(appointmentRepository.findById(appointmentId).orElseThrow());
    }

    private void rescheduleWithHumanAndPhysicalValidation(
            Appointment appointment,
            UUID appointmentId,
            RescheduleAppointmentCommand command,
            BookingChannel channel,
            String n8nConversationId
    ) {
        HumanResourceBookingContext context = HumanResourceBookingContext.forBooking(
                appointment.getPatientId(),
                command.medicoId(),
                appointment.getSecondaryDoctorId(),
                command.scheduleId(),
                command.sedeId(),
                command.appointmentDate(),
                command.appointmentTime(),
                appointment.getAppointmentType(),
                appointment.getDurationMinutes()
        );

        humanResourceAvailabilityService.assertRescheduleAllowed(context, appointmentId);

        boolean therapySlotChanged = appointment.getAppointmentType() == AppointmentType.TERAPIA_FISICA
                || appointment.getAppointmentType() == AppointmentType.TERAPIA_OCUPACIONAL;
        if (therapySlotChanged && !sameTherapySlot(appointment, command)) {
            humanResourceAvailabilityService.assertTherapyGroupAllowsNewPatient(
                    command.scheduleId(),
                    command.appointmentDate(),
                    command.appointmentTime(),
                    appointment.getAppointmentType()
            );
        }

        resourceCapacityService.release(appointmentId);
        appointment.reschedule(
                command.appointmentDate(),
                command.appointmentTime(),
                command.scheduleId(),
                command.medicoId(),
                command.sedeId(),
                channel,
                n8nConversationId
        );
        Appointment saved = appointmentRepository.save(appointment);
        resourceCapacityService.allocate(saved);
    }

    @Transactional
    public AppointmentDTO checkInAppointment(UUID appointmentId) {
        Appointment appointment = requireAppointmentForClinicalAction(appointmentId);
        appointment.checkIn();
        return appointmentEnrichmentService.toDto(appointmentRepository.save(appointment));
    }

    @Transactional
    public AppointmentDTO markNoShow(UUID appointmentId) {
        Appointment appointment = requireWritableAppointment(appointmentId);
        appointment.markNoShow();
        return appointmentEnrichmentService.toDto(appointmentRepository.save(appointment));
    }

    @Transactional
    public AppointmentDTO completeAppointment(UUID appointmentId) {
        Appointment appointment = requireAppointmentForClinicalAction(appointmentId);
        appointment.complete();
        return appointmentEnrichmentService.toDto(appointmentRepository.save(appointment));
    }

    @Transactional(readOnly = true)
    public List<AppointmentDTO> listPendingGroupTherapyAppointments() {
        if (!staffSecurityHelper.hasAnyRole(RoleName.APPOINTMENT_OPERATORS)) {
            throw new AccessDeniedException("Solo roles operativos pueden consultar terapias pendientes");
        }
        List<Appointment> pending = appointmentRepository
                .findByStatusAndAppointmentTypeIn(
                        AppointmentStatus.PENDIENTE_CONFIRMACION_GRUPO,
                        List.of(
                                com.ipscentir.appointments.domain.model.appointment.AppointmentType.TERAPIA_FISICA,
                                com.ipscentir.appointments.domain.model.appointment.AppointmentType.TERAPIA_OCUPACIONAL
                        )
                )
                .stream()
                .filter(this::canRead)
                .toList();
        return appointmentEnrichmentService.toDtos(pending);
    }

    @Transactional
    public int processPendingGroupTherapyCutoff() {
        if (!staffSecurityHelper.hasRole(RoleName.ADMINISTRACION)) {
            throw new AccessDeniedException("Solo Administracion puede ejecutar el corte de terapia grupal");
        }
        List<Appointment> before = appointmentRepository.findByStatusAndAppointmentTypeIn(
                AppointmentStatus.PENDIENTE_CONFIRMACION_GRUPO,
                List.of(
                        com.ipscentir.appointments.domain.model.appointment.AppointmentType.TERAPIA_FISICA,
                        com.ipscentir.appointments.domain.model.appointment.AppointmentType.TERAPIA_OCUPACIONAL
                )
        );
        int pendingBefore = before.size();
        therapyPendingGroupCutoffService.processPendingTherapySlots();
        List<Appointment> after = appointmentRepository.findByStatusAndAppointmentTypeIn(
                AppointmentStatus.PENDIENTE_CONFIRMACION_GRUPO,
                List.of(
                        com.ipscentir.appointments.domain.model.appointment.AppointmentType.TERAPIA_FISICA,
                        com.ipscentir.appointments.domain.model.appointment.AppointmentType.TERAPIA_OCUPACIONAL
                )
        );
        return Math.max(0, pendingBefore - after.size());
    }

    private AppointmentSearchCriteria applyRoleFilters(AppointmentSearchCriteria criteria) {
        if (staffSecurityHelper.hasRole(RoleName.MEDICO)) {
            String doctorId = staffSecurityHelper.requireDoctorIdForMedico();
            return new AppointmentSearchCriteria(
                    criteria.sedeId(),
                    doctorId,
                    criteria.patientId(),
                    criteria.status(),
                    criteria.bookingChannel(),
                    criteria.fromDate(),
                    criteria.toDate()
            );
        }
        return criteria;
    }

    private Appointment requireReadableAppointment(UUID appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found"));
        if (!canRead(appointment)) {
            throw new AccessDeniedException("No tiene permiso para ver esta cita");
        }
        return appointment;
    }

    private Appointment requireWritableAppointment(UUID appointmentId) {
        requireWriteRole();
        Appointment appointment = requireReadableAppointment(appointmentId);
        if (staffSecurityHelper.hasRole(RoleName.MEDICO)) {
            throw new AccessDeniedException("El rol Medico no puede modificar citas desde admisiones");
        }
        return appointment;
    }

    private Appointment requireAppointmentForClinicalAction(UUID appointmentId) {
        Appointment appointment = requireReadableAppointment(appointmentId);
        if (staffSecurityHelper.hasAnyRole(RoleName.APPOINTMENT_OPERATORS)) {
            return appointment;
        }
        if (staffSecurityHelper.hasRole(RoleName.MEDICO)
                && appointment.isAssignedToDoctor(staffSecurityHelper.requireDoctorIdForMedico())) {
            return appointment;
        }
        throw new AccessDeniedException("No tiene permiso para esta acción clínica sobre la cita");
    }

    private boolean canRead(Appointment appointment) {
        if (staffSecurityHelper.hasAnyRole(RoleName.APPOINTMENT_READERS)) {
            return sedeAuthorizationService.canAccessSede(appointment.getSedeId());
        }
        if (staffSecurityHelper.hasRole(RoleName.MEDICO)) {
            return appointment.isAssignedToDoctor(staffSecurityHelper.requireDoctorIdForMedico())
                    && sedeAuthorizationService.canAccessSede(appointment.getSedeId());
        }
        return false;
    }

    private static boolean sameTherapySlot(Appointment appointment, RescheduleAppointmentCommand command) {
        return appointment.getScheduleId().equals(command.scheduleId())
                && appointment.getAppointmentDate().equals(command.appointmentDate())
                && appointment.getAppointmentTime().equals(command.appointmentTime());
    }

    private void requireWriteRole() {
        if (!staffSecurityHelper.hasAnyRole(RoleName.APPOINTMENT_OPERATORS)) {
            throw new AccessDeniedException("Solo Administracion, Admisiones o Asesor pueden modificar citas");
        }
    }

    private AppointmentRepository.AppointmentSearchFilter toFilter(AppointmentSearchCriteria criteria) {
        return new AppointmentRepository.AppointmentSearchFilter(
                criteria.sedeId(),
                criteria.medicoId(),
                criteria.patientId(),
                criteria.status(),
                criteria.bookingChannel(),
                criteria.fromDate(),
                criteria.toDate()
        );
    }
}
