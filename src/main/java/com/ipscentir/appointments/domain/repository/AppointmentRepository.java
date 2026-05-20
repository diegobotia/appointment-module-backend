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
    
    List<Appointment> findByDoctorIdAndDate(String doctorId, LocalDate date);
    
    Appointment save(Appointment appointment);
    
    Optional<Appointment> findById(UUID id);
    
    boolean existsByPatientIdAndDate(UUID patientId, LocalDate date);

    List<Appointment> findByScheduleAndDateAndTimeAndType(UUID scheduleId, LocalDate date, LocalTime time, AppointmentType type);

    List<Appointment> findByStatusAndAppointmentTypeIn(AppointmentStatus status, List<AppointmentType> appointmentTypes);

    List<Appointment> findByScheduleAndDateAndTimeAndTypeForUpdate(UUID scheduleId, LocalDate date, LocalTime time, AppointmentType type);

    List<Appointment> findByDateAndStatusIn(LocalDate date, List<AppointmentStatus> statuses);

    List<Appointment> findByPatientIdOrderByAppointmentDateDescAppointmentTimeDesc(UUID patientId);

    List<Appointment> search(AppointmentSearchFilter filter);

    void saveAll(List<Appointment> appointments);

    long countByStatus(AppointmentStatus status);

    long countByAppointmentDate(LocalDate date);

    long countByAppointmentDateAndStatus(LocalDate date, AppointmentStatus status);

    long countByAppointmentDateBetween(LocalDate fromDate, LocalDate toDate);

    long countByAppointmentDateBetweenAndStatus(LocalDate fromDate, LocalDate toDate, AppointmentStatus status);

    record AppointmentSearchFilter(
            UUID facilityId,
            String doctorId,
            UUID patientId,
            AppointmentStatus status,
            java.time.LocalDate fromDate,
            java.time.LocalDate toDate
    ) {
    }
}
