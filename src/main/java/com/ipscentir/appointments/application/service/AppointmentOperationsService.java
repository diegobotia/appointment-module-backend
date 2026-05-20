package com.ipscentir.appointments.application.service;

import com.ipscentir.appointments.application.dto.AppointmentDTO;
import com.ipscentir.appointments.application.dto.AppointmentSearchCriteria;
import com.ipscentir.appointments.application.dto.CancelAppointmentCommand;
import com.ipscentir.appointments.application.dto.CreateAppointmentCommand;
import com.ipscentir.appointments.application.dto.RescheduleAppointmentCommand;
import com.ipscentir.appointments.application.mapper.AppointmentMapper;
import com.ipscentir.appointments.application.security.FacilityAuthorizationService;
import com.ipscentir.appointments.application.security.StaffSecurityHelper;
import com.ipscentir.appointments.domain.model.appointment.Appointment;
import com.ipscentir.appointments.domain.model.appointment.AppointmentStatus;
import com.ipscentir.appointments.domain.model.security.RoleName;
import com.ipscentir.appointments.domain.repository.AppointmentRepository;
import com.ipscentir.appointments.domain.service.AppointmentBookingService;
import com.ipscentir.appointments.domain.service.AvailabilityService;
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
    private final AppointmentMapper appointmentMapper;
    private final AvailabilityService availabilityService;
    private final FacilityAuthorizationService facilityAuthorizationService;
    private final StaffSecurityHelper staffSecurityHelper;
    private final TherapyPendingGroupCutoffService therapyPendingGroupCutoffService;

    @Transactional(readOnly = true)
    public List<AppointmentDTO> searchAppointments(AppointmentSearchCriteria criteria) {
        AppointmentSearchCriteria effective = applyRoleFilters(criteria);
        List<Appointment> appointments = appointmentRepository.search(toFilter(effective));
        return appointments.stream()
                .filter(this::canRead)
                .map(appointmentMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public AppointmentDTO getAppointment(UUID appointmentId) {
        Appointment appointment = requireReadableAppointment(appointmentId);
        return appointmentMapper.toDto(appointment);
    }

    @Transactional
    public AppointmentDTO createAppointment(CreateAppointmentCommand command) {
        requireWriteRole();
        facilityAuthorizationService.assertCurrentUserCanAccessFacility(command.facilityId());
        return appointmentApplicationService.createAppointment(command);
    }

    @Transactional
    public AppointmentDTO confirmAppointment(UUID appointmentId) {
        Appointment appointment = requireWritableAppointment(appointmentId);
        appointment.confirm();
        return appointmentMapper.toDto(appointmentRepository.save(appointment));
    }

    @Transactional
    public AppointmentDTO cancelAppointment(UUID appointmentId, CancelAppointmentCommand command) {
        Appointment appointment = requireWritableAppointment(appointmentId);
        facilityAuthorizationService.assertCurrentUserCanAccessFacility(appointment.getFacilityId());
        Appointment cancelled = appointmentBookingService.cancelAppointment(appointmentId, command.reason());
        return appointmentMapper.toDto(cancelled);
    }

    @Transactional
    public AppointmentDTO rescheduleAppointment(UUID appointmentId, RescheduleAppointmentCommand command) {
        Appointment appointment = requireWritableAppointment(appointmentId);
        facilityAuthorizationService.assertCurrentUserCanAccessFacility(command.facilityId());
        facilityAuthorizationService.assertCurrentUserCanAccessFacility(appointment.getFacilityId());

        if (!availabilityService.isSlotAvailable(
                command.doctorId(),
                command.facilityId(),
                command.appointmentDate(),
                command.appointmentTime()
        )) {
            throw new IllegalStateException("The requested slot is not available for rescheduling");
        }

        appointment.reschedule(
                command.appointmentDate(),
                command.appointmentTime(),
                command.scheduleId(),
                command.doctorId(),
                command.facilityId()
        );
        return appointmentMapper.toDto(appointmentRepository.save(appointment));
    }

    @Transactional
    public AppointmentDTO checkInAppointment(UUID appointmentId) {
        Appointment appointment = requireAppointmentForClinicalAction(appointmentId);
        appointment.checkIn();
        return appointmentMapper.toDto(appointmentRepository.save(appointment));
    }

    @Transactional
    public AppointmentDTO markNoShow(UUID appointmentId) {
        Appointment appointment = requireWritableAppointment(appointmentId);
        appointment.markNoShow();
        return appointmentMapper.toDto(appointmentRepository.save(appointment));
    }

    @Transactional
    public AppointmentDTO completeAppointment(UUID appointmentId) {
        Appointment appointment = requireAppointmentForClinicalAction(appointmentId);
        appointment.complete();
        return appointmentMapper.toDto(appointmentRepository.save(appointment));
    }

    @Transactional(readOnly = true)
    public List<AppointmentDTO> listPendingGroupTherapyAppointments() {
        if (!staffSecurityHelper.hasAnyRole(RoleName.ADMINISTRACION, RoleName.ADMISIONES)) {
            throw new AccessDeniedException("Solo Administracion o Admisiones pueden consultar terapias pendientes");
        }
        return appointmentRepository
                .findByStatusAndAppointmentTypeIn(
                        AppointmentStatus.PENDIENTE_CONFIRMACION_GRUPO,
                        List.of(
                                com.ipscentir.appointments.domain.model.appointment.AppointmentType.TERAPIA_FISICA,
                                com.ipscentir.appointments.domain.model.appointment.AppointmentType.TERAPIA_OCUPACIONAL
                        )
                )
                .stream()
                .filter(this::canRead)
                .map(appointmentMapper::toDto)
                .toList();
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
                    criteria.facilityId(),
                    doctorId,
                    criteria.patientId(),
                    criteria.status(),
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
        if (staffSecurityHelper.hasAnyRole(RoleName.ADMINISTRACION, RoleName.ADMISIONES)) {
            return appointment;
        }
        if (staffSecurityHelper.hasRole(RoleName.MEDICO)
                && appointment.isAssignedToDoctor(staffSecurityHelper.requireDoctorIdForMedico())) {
            return appointment;
        }
        throw new AccessDeniedException("No tiene permiso para esta acción clínica sobre la cita");
    }

    private boolean canRead(Appointment appointment) {
        if (staffSecurityHelper.hasAnyRole(RoleName.ADMINISTRACION, RoleName.ADMISIONES, RoleName.FACTURACION)) {
            return facilityAuthorizationService.canAccessFacility(appointment.getFacilityId());
        }
        if (staffSecurityHelper.hasRole(RoleName.MEDICO)) {
            return appointment.isAssignedToDoctor(staffSecurityHelper.requireDoctorIdForMedico())
                    && facilityAuthorizationService.canAccessFacility(appointment.getFacilityId());
        }
        return false;
    }

    private void requireWriteRole() {
        if (!staffSecurityHelper.hasAnyRole(RoleName.ADMINISTRACION, RoleName.ADMISIONES)) {
            throw new AccessDeniedException("Solo Administracion o Admisiones pueden modificar citas");
        }
    }

    private AppointmentRepository.AppointmentSearchFilter toFilter(AppointmentSearchCriteria criteria) {
        return new AppointmentRepository.AppointmentSearchFilter(
                criteria.facilityId(),
                criteria.doctorId(),
                criteria.patientId(),
                criteria.status(),
                criteria.fromDate(),
                criteria.toDate()
        );
    }
}
