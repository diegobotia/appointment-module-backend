package com.ipscentir.appointments.application.service;

import com.ipscentir.appointments.application.dto.AppointmentDTO;
import com.ipscentir.appointments.application.dto.AppointmentSearchCriteria;
import com.ipscentir.appointments.application.dto.AvailableSlotDTO;
import com.ipscentir.appointments.application.dto.ScheduleDTO;
import com.ipscentir.appointments.application.dto.availability.MedicoAvailabilityResponse;
import com.ipscentir.appointments.application.dto.schedule.MyScheduleResponse;
import com.ipscentir.appointments.application.mapper.ScheduleMapper;
import com.ipscentir.appointments.application.security.SedeAuthorizationService;
import com.ipscentir.appointments.application.security.StaffSecurityHelper;
import com.ipscentir.appointments.domain.model.schedule.AvailableSlot;
import com.ipscentir.appointments.domain.model.schedule.Schedule;
import com.ipscentir.appointments.domain.model.security.RoleName;
import com.ipscentir.appointments.domain.repository.ScheduleRepository;
import com.ipscentir.appointments.domain.service.AvailabilityService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ScheduleApplicationService {

    private final ScheduleRepository scheduleRepository;
    private final ScheduleMapper scheduleMapper;
    private final SedeAuthorizationService sedeAuthorizationService;
    private final AvailabilityService availabilityService;
    private final MedicoApplicationService medicoApplicationService;
    private final AppointmentOperationsService appointmentOperationsService;
    private final StaffSecurityHelper staffSecurityHelper;

    @Transactional(readOnly = true)
    public ScheduleDTO getScheduleForMedicoAndFacility(String medicoId, Integer sedeId, DayOfWeek dayOfWeek) {
        assertCanViewMedicoSchedule(medicoId);
        sedeAuthorizationService.assertCurrentUserCanAccessSede(sedeId);
        Schedule schedule = scheduleRepository.findByDoctorIdAndSedeIdAndDayOfWeek(medicoId, sedeId, dayOfWeek)
                .orElseThrow(() -> new IllegalArgumentException("No schedule found for this medico, facility, and specified day"));

        return scheduleMapper.toDto(schedule);
    }

    @Transactional(readOnly = true)
    public List<ScheduleDTO> listScheduleTemplatesForMedico(String medicoId, Integer sedeId) {
        assertCanViewMedicoSchedule(medicoId);
        sedeAuthorizationService.assertCurrentUserCanAccessSede(sedeId);
        return scheduleRepository.findByDoctorIdAndSedeId(medicoId, sedeId).stream()
                .map(scheduleMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AvailableSlotDTO> getAvailabilityForDate(String medicoId, Integer sedeId, LocalDate date) {
        assertCanViewMedicoSchedule(medicoId);
        sedeAuthorizationService.assertCurrentUserCanAccessSede(sedeId);
        List<AvailableSlot> slots = availabilityService.getAvailableSlots(medicoId, sedeId, date);
        return slots.stream().map(scheduleMapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public MedicoAvailabilityResponse getAvailabilityInRange(
            String medicoId,
            Integer sedeId,
            LocalDate fromDate,
            LocalDate toDate
    ) {
        return medicoApplicationService.getMedicoAvailability(medicoId, sedeId, fromDate, toDate);
    }

    @Transactional(readOnly = true)
    public MyScheduleResponse getMySchedule(Integer sedeId, LocalDate fromDate, LocalDate toDate) {
        if (!staffSecurityHelper.hasRole(RoleName.MEDICO)) {
            throw new AccessDeniedException("Solo médicos pueden consultar /me/schedule");
        }
        String medicoId = staffSecurityHelper.requireDoctorIdForMedico();
        sedeAuthorizationService.assertCurrentUserCanAccessSede(sedeId);

        LocalDate from = fromDate != null ? fromDate : LocalDate.now();
        LocalDate to = toDate != null ? toDate : from.plusDays(6);

        List<ScheduleDTO> templates = listScheduleTemplatesForMedico(medicoId, sedeId);
        List<AppointmentDTO> appointments = appointmentOperationsService.searchAppointments(
                new AppointmentSearchCriteria(sedeId, medicoId, null, null, null, from, to)
        );

        return new MyScheduleResponse(medicoId, from, to, templates, appointments);
    }

    private void assertCanViewMedicoSchedule(String medicoId) {
        if (staffSecurityHelper.hasRole(RoleName.MEDICO)) {
            String ownMedicoId = staffSecurityHelper.requireDoctorIdForMedico();
            if (!ownMedicoId.equals(medicoId)) {
                throw new AccessDeniedException("El médico solo puede consultar su propia agenda");
            }
        }
    }
}
