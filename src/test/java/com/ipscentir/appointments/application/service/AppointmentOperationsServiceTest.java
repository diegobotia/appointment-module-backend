package com.ipscentir.appointments.application.service;

import com.ipscentir.appointments.domain.model.facility.FacilityMasterData;

import com.ipscentir.appointments.application.dto.AppointmentDTO;
import com.ipscentir.appointments.application.dto.AppointmentSearchCriteria;
import com.ipscentir.appointments.application.dto.CreateAppointmentCommand;
import com.ipscentir.appointments.application.mapper.AppointmentMapper;
import com.ipscentir.appointments.application.security.SedeAuthorizationService;
import com.ipscentir.appointments.application.security.StaffSecurityHelper;
import com.ipscentir.appointments.domain.model.appointment.Appointment;
import com.ipscentir.appointments.domain.model.appointment.AppointmentScheduleData;
import com.ipscentir.appointments.domain.model.appointment.AppointmentStatus;
import com.ipscentir.appointments.domain.model.appointment.AppointmentType;
import com.ipscentir.appointments.domain.model.appointment.BookingChannel;
import com.ipscentir.appointments.domain.model.security.RoleName;
import com.ipscentir.appointments.domain.repository.AppointmentRepository;
import com.ipscentir.appointments.domain.service.AppointmentBookingService;
import com.ipscentir.appointments.domain.service.HumanResourceAvailabilityService;
import com.ipscentir.appointments.domain.service.ResourceCapacityService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppointmentOperationsServiceTest {

    @Mock
    private AppointmentRepository appointmentRepository;
    @Mock
    private AppointmentApplicationService appointmentApplicationService;
    @Mock
    private AppointmentBookingService appointmentBookingService;
    @Mock
    private AppointmentMapper appointmentMapper;
    @Mock
    private HumanResourceAvailabilityService humanResourceAvailabilityService;
    @Mock
    private ResourceCapacityService resourceCapacityService;
    @Mock
    private SedeAuthorizationService sedeAuthorizationService;
    @Mock
    private StaffSecurityHelper staffSecurityHelper;
    @Mock
    private TherapyPendingGroupCutoffService therapyPendingGroupCutoffService;

    @InjectMocks
    private AppointmentOperationsService appointmentOperationsService;

    @Test
    void searchAppointmentsForMedicoForcesOwnDoctorId() {
        Integer sedeId = FacilityMasterData.SEDE_ID_BELEN;
        String medicoId = UUID.randomUUID().toString();
        LocalDate from = LocalDate.now();
        Appointment appointment = sampleAppointment(medicoId, sedeId);

        when(staffSecurityHelper.hasRole(RoleName.MEDICO)).thenReturn(true);
        when(staffSecurityHelper.requireDoctorIdForMedico()).thenReturn(medicoId);
        when(appointmentRepository.search(any())).thenReturn(List.of(appointment));
        when(staffSecurityHelper.hasAnyRole(RoleName.APPOINTMENT_READERS))
                .thenReturn(false);
        when(staffSecurityHelper.hasRole(RoleName.MEDICO)).thenReturn(true);
        when(sedeAuthorizationService.canAccessSede(sedeId)).thenReturn(true);
        when(appointmentMapper.toDto(appointment)).thenReturn(sampleDto(appointment));

        List<AppointmentDTO> result = appointmentOperationsService.searchAppointments(
                new AppointmentSearchCriteria(sedeId, "other-doctor", null, null, null, from, from)
        );

        assertEquals(1, result.size());
        verify(appointmentRepository).search(any());
    }

    @Test
    void confirmAppointmentRequiresWriteRole() {
        UUID appointmentId = UUID.randomUUID();
        when(staffSecurityHelper.hasAnyRole(RoleName.APPOINTMENT_OPERATORS)).thenReturn(false);

        assertThrows(AccessDeniedException.class, () -> appointmentOperationsService.confirmAppointment(appointmentId));
    }

    @Test
    void confirmAppointmentForAdmisionesPersistsChange() {
        UUID appointmentId = UUID.randomUUID();
        Integer sedeId = FacilityMasterData.SEDE_ID_BELEN;
        String doctorId = UUID.randomUUID().toString();
        Appointment appointment = sampleAppointment(doctorId, sedeId);

        when(staffSecurityHelper.hasAnyRole(RoleName.APPOINTMENT_OPERATORS)).thenReturn(true);
        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(appointment));
        when(staffSecurityHelper.hasAnyRole(RoleName.APPOINTMENT_READERS))
                .thenReturn(true);
        when(sedeAuthorizationService.canAccessSede(sedeId)).thenReturn(true);
        when(appointmentRepository.save(appointment)).thenReturn(appointment);
        when(appointmentMapper.toDto(appointment)).thenAnswer(invocation -> sampleDto((Appointment) invocation.getArgument(0)));

        AppointmentDTO dto = appointmentOperationsService.confirmAppointment(appointmentId);

        assertEquals(AppointmentStatus.CONFIRMED, dto.status());
        assertEquals(AppointmentStatus.CONFIRMED, appointment.getStatus());
    }

    @Test
    void getAppointmentDelegatesToRepository() {
        UUID appointmentId = UUID.randomUUID();
        Integer sedeId = FacilityMasterData.SEDE_ID_BELEN;
        String doctorId = UUID.randomUUID().toString();
        Appointment appointment = sampleAppointment(doctorId, sedeId);

        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(appointment));
        when(staffSecurityHelper.hasAnyRole(RoleName.APPOINTMENT_READERS))
                .thenReturn(true);
        when(sedeAuthorizationService.canAccessSede(sedeId)).thenReturn(true);
        when(appointmentMapper.toDto(appointment)).thenReturn(sampleDto(appointment));

        AppointmentDTO dto = appointmentOperationsService.getAppointment(appointmentId);

        assertEquals(appointment.getId(), dto.id());
    }

    @Test
    void createAppointmentSetsStaffBookingChannel() {
        Integer sedeId = FacilityMasterData.SEDE_ID_BELEN;
        CreateAppointmentCommand command = new CreateAppointmentCommand(
                UUID.randomUUID(),
                UUID.randomUUID().toString(),
                sedeId,
                null,
                UUID.randomUUID(),
                LocalDate.now().plusDays(2),
                LocalTime.of(10, 0),
                "PRESENCIAL",
                "Motivo",
                null,
                null
        );
        AppointmentDTO created = sampleDto(sampleAppointment(command.doctorId(), sedeId));

        when(staffSecurityHelper.hasAnyRole(RoleName.APPOINTMENT_OPERATORS)).thenReturn(true);
        when(appointmentApplicationService.createAppointment(any())).thenReturn(created);

        AppointmentDTO result = appointmentOperationsService.createAppointment(command);

        assertEquals(created.id(), result.id());
        verify(appointmentApplicationService).createAppointment(any());
    }

    private static Appointment sampleAppointment(String doctorId, Integer sedeId) {
        return Appointment.scheduleNew(
                UUID.randomUUID(),
                doctorId,
                null,
                new AppointmentScheduleData(
                        UUID.randomUUID(),
                        sedeId,
                        LocalDate.now().plusDays(1),
                        LocalTime.of(9, 0),
                        30,
                        AppointmentType.PRESENCIAL,
                        AppointmentStatus.SCHEDULED,
                        "Test"
                )
        );
    }

    private static AppointmentDTO sampleDto(Appointment appointment) {
        return new AppointmentDTO(
                appointment.getId(),
                appointment.getPatientId(),
                appointment.getDoctorId(),
                appointment.getSedeId(),
                null,
                appointment.getScheduleId(),
                appointment.getAppointmentDate(),
                appointment.getAppointmentTime(),
                appointment.getDurationMinutes(),
                appointment.getAppointmentType(),
                appointment.getStatus(),
                appointment.getBookingChannel(),
                appointment.getN8nConversationId(),
                appointment.getReason(),
                null,
                null,
                null
        );
    }
}
