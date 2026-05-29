package com.ipscentir.appointments.domain.service;

import com.ipscentir.appointments.domain.model.facility.FacilityMasterData;

import com.ipscentir.appointments.application.exception.ResourceCapacityExceededException;
import com.ipscentir.appointments.domain.model.appointment.Appointment;
import com.ipscentir.appointments.domain.model.appointment.AppointmentScheduleData;
import com.ipscentir.appointments.domain.model.appointment.AppointmentStatus;
import com.ipscentir.appointments.domain.model.appointment.AppointmentType;
import com.ipscentir.appointments.domain.model.facility.AppointmentResourceAllocation;
import com.ipscentir.appointments.domain.model.sede.Sede;
import com.ipscentir.appointments.domain.model.facility.FacilityResourceType;
import com.ipscentir.appointments.domain.repository.AppointmentResourceAllocationRepository;
import com.ipscentir.appointments.domain.repository.SedeRepository;
import com.ipscentir.appointments.domain.repository.FacilityResourceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResourceCapacityServiceTest {

    @Mock
    private FacilityResourceRepository facilityResourceRepository;

    @Mock
    private SedeRepository sedeRepository;

    @Mock
    private AppointmentResourceAllocationRepository allocationRepository;

    @InjectMocks
    private ResourceCapacityService resourceCapacityService;

    private final Integer sedeId = FacilityMasterData.SEDE_ID_BELEN;
    private final UUID scheduleId = UUID.randomUUID();
    private final LocalDate date = LocalDate.now().plusDays(3);
    private final LocalTime time = LocalTime.of(10, 0);

    @Test
    void rejectsWhenConsultorioCapacityExceeded() {
        when(facilityResourceRepository.countActiveBySedeIdAndResourceType(sedeId, FacilityResourceType.CONSULTORIO))
                .thenReturn(4L);
        when(allocationRepository.findOccupiedForUpdate(
                eq(sedeId), eq(FacilityResourceType.CONSULTORIO), eq(date), eq(time), any()))
                .thenReturn(List.of(
                        buildAllocation("key-1"),
                        buildAllocation("key-2"),
                        buildAllocation("key-3"),
                        buildAllocation("key-4")
                ));
        when(sedeRepository.findById(sedeId)).thenReturn(Optional.of(
                Sede.builder().id(sedeId).nombre("Sede Test").build()
        ));

        assertThrows(ResourceCapacityExceededException.class, () -> resourceCapacityService.assertCanAllocate(
                sedeId,
                AppointmentType.PRESENCIAL,
                UUID.randomUUID(),
                date,
                time,
                30,
                null
        ));
    }

    private AppointmentResourceAllocation buildAllocation(String sessionKey) {
        return AppointmentResourceAllocation.builder()
                .appointmentId(UUID.randomUUID())
                .capacitySessionKey(sessionKey)
                .resourceType(FacilityResourceType.CONSULTORIO)
                .build();
    }

    @Test
    void allowsTherapyPatientJoiningExistingSession() {
        String sessionKey = sedeId + "|" + date + "|" + time + "|" + AppointmentType.TERAPIA_FISICA + "|" + scheduleId;
        when(facilityResourceRepository.countActiveBySedeIdAndResourceType(sedeId, FacilityResourceType.FISIOTERAPIA))
                .thenReturn(2L);
        when(allocationRepository.existsActiveSessionKey(sessionKey)).thenReturn(true);

        assertDoesNotThrow(() -> resourceCapacityService.assertCanAllocate(
                sedeId,
                AppointmentType.TERAPIA_FISICA,
                scheduleId,
                date,
                time,
                30,
                null
        ));
    }

    @Test
    void allocatesAndReleasesAppointmentResource() {
        Appointment appointment = Appointment.scheduleNew(
                UUID.randomUUID(),
                "doc-1",
                null,
                new AppointmentScheduleData(
                        scheduleId,
                        sedeId,
                        date,
                        time,
                        30,
                        AppointmentType.PRESENCIAL,
                        AppointmentStatus.SCHEDULED,
                        "Control"
                )
        );

        when(facilityResourceRepository.countActiveBySedeIdAndResourceType(sedeId, FacilityResourceType.CONSULTORIO))
                .thenReturn(4L);
        when(allocationRepository.save(any(AppointmentResourceAllocation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        resourceCapacityService.allocate(appointment);
        verify(allocationRepository).save(any(AppointmentResourceAllocation.class));

        UUID savedAppointmentId = appointment.getId();
        AppointmentResourceAllocation active = AppointmentResourceAllocation.builder()
                .appointmentId(savedAppointmentId)
                .sedeId(sedeId)
                .resourceType(FacilityResourceType.CONSULTORIO)
                .appointmentDate(date)
                .startTime(time)
                .endTime(time.plusMinutes(30))
                .capacitySessionKey(sedeId + "|" + savedAppointmentId)
                .build();
        when(allocationRepository.findActiveByAppointmentId(savedAppointmentId)).thenReturn(Optional.of(active));
        when(allocationRepository.save(active)).thenReturn(active);

        resourceCapacityService.release(savedAppointmentId);
        verify(allocationRepository).save(active);
    }
}
