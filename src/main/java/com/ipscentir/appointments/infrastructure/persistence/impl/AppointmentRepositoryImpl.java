package com.ipscentir.appointments.infrastructure.persistence.impl;

import com.ipscentir.appointments.domain.model.appointment.Appointment;
import com.ipscentir.appointments.domain.model.appointment.AppointmentStatus;
import com.ipscentir.appointments.domain.model.appointment.AppointmentType;
import com.ipscentir.appointments.domain.repository.AppointmentRepository;
import com.ipscentir.appointments.infrastructure.persistence.jpa.AppointmentJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AppointmentRepositoryImpl implements AppointmentRepository {

    private final AppointmentJpaRepository jpaRepository;

    @Override
    public List<Appointment> findByDoctorIdAndDate(UUID doctorId, LocalDate date) {
        return jpaRepository.findByParticipantDoctorIdAndAppointmentDate(doctorId, date);
    }

    @Override
    public Appointment save(Appointment appointment) {
        return jpaRepository.save(appointment);
    }

    @Override
    public Optional<Appointment> findById(UUID id) {
        return jpaRepository.findById(id);
    }

    @Override
    public boolean existsByPatientIdAndDate(UUID patientId, LocalDate date) {
        return jpaRepository.existsByPatientIdAndAppointmentDateAndActiveStatus(patientId, date);
    }

    @Override
    public List<Appointment> findByScheduleAndDateAndTimeAndType(UUID scheduleId, LocalDate date, LocalTime time, AppointmentType type) {
        return jpaRepository.findByScheduleIdAndAppointmentDateAndAppointmentTimeAndAppointmentType(scheduleId, date, time, type);
    }

    @Override
    public List<Appointment> findByStatusAndAppointmentTypeIn(AppointmentStatus status, List<AppointmentType> appointmentTypes) {
        return jpaRepository.findByStatusAndAppointmentTypeIn(status, appointmentTypes);
    }

    @Override
    public List<Appointment> findByScheduleAndDateAndTimeAndTypeForUpdate(UUID scheduleId, LocalDate date, LocalTime time, AppointmentType type) {
        return jpaRepository.findByScheduleIdAndAppointmentDateAndAppointmentTimeAndAppointmentTypeForUpdate(scheduleId, date, time, type);
    }

    @Override
    public void saveAll(List<Appointment> appointments) {
        jpaRepository.saveAll(appointments);
    }
}
