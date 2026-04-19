package com.ipscentir.appointments.application.service;

import com.ipscentir.appointments.application.dto.AppointmentDTO;
import com.ipscentir.appointments.application.dto.CreateAppointmentCommand;
import com.ipscentir.appointments.application.mapper.AppointmentMapper;
import com.ipscentir.appointments.domain.model.appointment.Appointment;
import com.ipscentir.appointments.domain.model.appointment.AppointmentType;
import com.ipscentir.appointments.domain.service.AppointmentBookingRequest;
import com.ipscentir.appointments.domain.service.AppointmentBookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AppointmentApplicationService {

    private final AppointmentBookingService appointmentBookingService;
    private final AppointmentMapper appointmentMapper;

    public AppointmentDTO createAppointment(CreateAppointmentCommand command) {
        
        // El Application Service recibe el Input DTO (Command), extrae los parámetros,
        // maneja la transacción orquestando el Core Domain (Booking Service),
        // y retorna un Output DTO.
        
        Appointment appointment = appointmentBookingService.bookAppointment(
            new AppointmentBookingRequest(
                command.patientId(),
                command.doctorId(),
                command.secondaryDoctorId(),
                command.scheduleId(),
                command.appointmentDate(),
                command.appointmentTime(),
                AppointmentType.valueOf(command.appointmentType().toUpperCase()),
                command.reason()
            )
        );

        return appointmentMapper.toDto(appointment);
    }
}
