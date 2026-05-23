package com.ipscentir.appointments.application.service;

import com.ipscentir.appointments.application.dto.AppointmentDTO;
import com.ipscentir.appointments.application.dto.AppointmentSearchCriteria;
import com.ipscentir.appointments.application.dto.AvailableSlotDTO;
import com.ipscentir.appointments.application.dto.ScheduleDTO;
import com.ipscentir.appointments.application.dto.availability.DoctorAvailabilityResponse;
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
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ScheduleApplicationService {

    private final ScheduleRepository scheduleRepository;
    private final ScheduleMapper scheduleMapper;
    private final SedeAuthorizationService sedeAuthorizationService;
    private final AvailabilityService availabilityService;
    private final DoctorApplicationService doctorApplicationService;
    private final AppointmentOperationsService appointmentOperationsService;
    private final StaffSecurityHelper staffSecurityHelper;

    @Transactional(readOnly = true)
    public ScheduleDTO getScheduleForDoctorAndFacility(String doctorId, Integer sedeId, DayOfWeek dayOfWeek) {
        assertCanViewDoctorSchedule(doctorId);
        sedeAuthorizationService.assertCurrentUserCanAccessSede(sedeId);
        Schedule schedule = scheduleRepository.findByDoctorIdAndSedeIdAndDayOfWeek(doctorId, sedeId, dayOfWeek)
                .orElseThrow(() -> new IllegalArgumentException("No schedule found for this doctor, facility, and specified day"));

        return scheduleMapper.toDto(schedule);
    }

    @Transactional(readOnly = true)
    public List<ScheduleDTO> listScheduleTemplatesForDoctor(String doctorId, Integer sedeId) {
        assertCanViewDoctorSchedule(doctorId);
        sedeAuthorizationService.assertCurrentUserCanAccessSede(sedeId);
        return scheduleRepository.findByDoctorIdAndSedeId(doctorId, sedeId).stream()
                .map(scheduleMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AvailableSlotDTO> getAvailabilityForDate(String doctorId, Integer sedeId, LocalDate date) {
        assertCanViewDoctorSchedule(doctorId);
        sedeAuthorizationService.assertCurrentUserCanAccessSede(sedeId);
        List<AvailableSlot> slots = availabilityService.getAvailableSlots(doctorId, sedeId, date);
        return slots.stream().map(scheduleMapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public DoctorAvailabilityResponse getAvailabilityInRange(
            String doctorId,
            Integer sedeId,
            LocalDate fromDate,
            LocalDate toDate
    ) {
        return doctorApplicationService.getDoctorAvailability(doctorId, sedeId, fromDate, toDate);
    }

    @Transactional(readOnly = true)
    public MyScheduleResponse getMySchedule(Integer sedeId, LocalDate fromDate, LocalDate toDate) {
        if (!staffSecurityHelper.hasRole(RoleName.MEDICO)) {
            throw new AccessDeniedException("Solo médicos pueden consultar /me/schedule");
        }
        String doctorId = staffSecurityHelper.requireDoctorIdForMedico();
        sedeAuthorizationService.assertCurrentUserCanAccessSede(sedeId);

        LocalDate from = fromDate != null ? fromDate : LocalDate.now();
        LocalDate to = toDate != null ? toDate : from.plusDays(6);

        List<ScheduleDTO> templates = listScheduleTemplatesForDoctor(doctorId, sedeId);
        List<AppointmentDTO> appointments = appointmentOperationsService.searchAppointments(
                new AppointmentSearchCriteria(sedeId, doctorId, null, null, null, from, to)
        );

        return new MyScheduleResponse(doctorId, from, to, templates, appointments);
    }

    private void assertCanViewDoctorSchedule(String doctorId) {
        if (staffSecurityHelper.hasRole(RoleName.MEDICO)) {
            String ownDoctorId = staffSecurityHelper.requireDoctorIdForMedico();
            if (!ownDoctorId.equals(doctorId)) {
                throw new AccessDeniedException("El médico solo puede consultar su propia agenda");
            }
        }
    }
}
