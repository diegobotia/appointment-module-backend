package com.ipscentir.appointments.infrastructure.persistence.jpa;

import com.ipscentir.appointments.domain.model.appointment.Appointment;
import com.ipscentir.appointments.domain.model.appointment.AppointmentStatus;
import com.ipscentir.appointments.domain.model.appointment.AppointmentType;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface AppointmentJpaRepository extends JpaRepository<Appointment, UUID> {

    @Query("SELECT DISTINCT a FROM Appointment a JOIN a.participants p WHERE p.doctorId = :doctorId AND a.appointmentDate = :appointmentDate")
    List<Appointment> findByParticipantDoctorIdAndAppointmentDate(String doctorId, LocalDate appointmentDate);

    @Query("SELECT COUNT(a) > 0 FROM Appointment a WHERE a.patientId = :patientId AND a.appointmentDate = :date AND a.status NOT IN ('CANCELLED', 'NO_SHOW')")
    boolean existsByPatientIdAndAppointmentDateAndActiveStatus(UUID patientId, LocalDate date);

    List<Appointment> findByStatusAndAppointmentTypeIn(AppointmentStatus status, List<AppointmentType> appointmentTypes);

    List<Appointment> findByScheduleIdAndAppointmentDateAndAppointmentTimeAndAppointmentType(UUID scheduleId, LocalDate date, LocalTime time, AppointmentType type);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Appointment a WHERE a.scheduleId = :scheduleId AND a.appointmentDate = :date AND a.appointmentTime = :time AND a.appointmentType = :type")
    List<Appointment> findByScheduleIdAndAppointmentDateAndAppointmentTimeAndAppointmentTypeForUpdate(
            UUID scheduleId,
            LocalDate date,
            LocalTime time,
            AppointmentType type
    );
}
