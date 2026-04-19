package com.ipscentir.appointments.domain.repository;

import com.ipscentir.appointments.domain.model.appointment.Appointment;
import com.ipscentir.appointments.domain.model.appointment.AppointmentStatus;
import com.ipscentir.appointments.domain.model.appointment.AppointmentType;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AppointmentRepository {
    
    List<Appointment> findByDoctorIdAndDate(UUID doctorId, LocalDate date);
    
    Appointment save(Appointment appointment);
    
    Optional<Appointment> findById(UUID id);
    
    boolean existsByPatientIdAndDate(UUID patientId, LocalDate date);

    List<Appointment> findByScheduleAndDateAndTimeAndType(UUID scheduleId, LocalDate date, LocalTime time, AppointmentType type);

    List<Appointment> findByStatusAndAppointmentTypeIn(AppointmentStatus status, List<AppointmentType> appointmentTypes);

    List<Appointment> findByScheduleAndDateAndTimeAndTypeForUpdate(UUID scheduleId, LocalDate date, LocalTime time, AppointmentType type);

    void saveAll(List<Appointment> appointments);
}
