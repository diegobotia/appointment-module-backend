package com.ipscentir.appointments.infrastructure.persistence.jpa;

import com.ipscentir.appointments.domain.model.appointment.Appointment;
import com.ipscentir.appointments.domain.model.appointment.AppointmentStatus;
import com.ipscentir.appointments.domain.model.appointment.AppointmentType;
import com.ipscentir.appointments.domain.model.appointment.BookingChannel;
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

    @Query("""
            SELECT COUNT(a) > 0 FROM Appointment a
            WHERE a.patientId = :patientId
              AND a.appointmentDate = :date
              AND a.status NOT IN ('CANCELLED', 'NO_SHOW')
              AND a.id <> :excludeAppointmentId
            """)
    boolean existsByPatientIdAndAppointmentDateAndActiveStatusExcluding(
            UUID patientId,
            LocalDate date,
            UUID excludeAppointmentId
    );

    List<Appointment> findByStatusAndAppointmentTypeIn(AppointmentStatus status, List<AppointmentType> appointmentTypes);

    List<Appointment> findByScheduleIdAndAppointmentDateAndAppointmentTimeAndAppointmentType(UUID scheduleId, LocalDate date, LocalTime time, AppointmentType type);

    List<Appointment> findByAppointmentDateAndStatusIn(LocalDate appointmentDate, List<AppointmentStatus> statuses);

    List<Appointment> findByPatientIdOrderByAppointmentDateDescAppointmentTimeDesc(UUID patientId);

    List<Appointment> findBySedeId(Integer sedeId);

    List<Appointment> findByDoctorId(String doctorId);

    List<Appointment> findByPatientId(UUID patientId);

    List<Appointment> findByStatus(AppointmentStatus status);

    List<Appointment> findByAppointmentDateBetween(LocalDate fromDate, LocalDate toDate);

    List<Appointment> findBySedeIdAndAppointmentDateBetween(Integer sedeId, LocalDate fromDate, LocalDate toDate);

    List<Appointment> findByDoctorIdAndAppointmentDateBetween(String doctorId, LocalDate fromDate, LocalDate toDate);

    long countByStatus(AppointmentStatus status);

    long countByAppointmentDate(LocalDate appointmentDate);

    long countByAppointmentDateAndStatus(LocalDate appointmentDate, AppointmentStatus status);

    long countByAppointmentDateBetween(LocalDate fromDate, LocalDate toDate);

    long countByAppointmentDateBetweenAndStatus(LocalDate fromDate, LocalDate toDate, AppointmentStatus status);

    long countByBookingChannel(BookingChannel bookingChannel);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Appointment a WHERE a.scheduleId = :scheduleId AND a.appointmentDate = :date AND a.appointmentTime = :time AND a.appointmentType = :type")
    List<Appointment> findByScheduleIdAndAppointmentDateAndAppointmentTimeAndAppointmentTypeForUpdate(
            UUID scheduleId,
            LocalDate date,
            LocalTime time,
            AppointmentType type
    );
}
