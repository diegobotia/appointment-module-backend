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
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

@Component
@RequiredArgsConstructor
public class AppointmentRepositoryImpl implements AppointmentRepository {

    private final AppointmentJpaRepository jpaRepository;

    @Override
    public List<Appointment> findByDoctorIdAndDate(String doctorId, LocalDate date) {
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
    public List<Appointment> findByDateAndStatusIn(LocalDate date, List<AppointmentStatus> statuses) {
        return jpaRepository.findByAppointmentDateAndStatusIn(date, statuses);
    }

    @Override
    public List<Appointment> findByPatientIdOrderByAppointmentDateDescAppointmentTimeDesc(UUID patientId) {
        return jpaRepository.findByPatientIdOrderByAppointmentDateDescAppointmentTimeDesc(patientId);
    }

    @Override
    public List<Appointment> search(AppointmentSearchFilter filter) {
        Stream<Appointment> stream = baseStream(filter);

        if (filter.facilityId() != null) {
            stream = stream.filter(a -> filter.facilityId().equals(a.getFacilityId()));
        }
        if (filter.doctorId() != null && !filter.doctorId().isBlank()) {
            stream = stream.filter(a -> a.isAssignedToDoctor(filter.doctorId()));
        }
        if (filter.patientId() != null) {
            stream = stream.filter(a -> filter.patientId().equals(a.getPatientId()));
        }
        if (filter.status() != null) {
            stream = stream.filter(a -> filter.status() == a.getStatus());
        }
        if (filter.fromDate() != null) {
            stream = stream.filter(a -> !a.getAppointmentDate().isBefore(filter.fromDate()));
        }
        if (filter.toDate() != null) {
            stream = stream.filter(a -> !a.getAppointmentDate().isAfter(filter.toDate()));
        }

        return stream
                .sorted(Comparator.comparing(Appointment::getAppointmentDate).reversed()
                        .thenComparing(Appointment::getAppointmentTime).reversed())
                .toList();
    }

    private Stream<Appointment> baseStream(AppointmentSearchFilter filter) {
        if (filter.doctorId() != null && !filter.doctorId().isBlank()
                && filter.fromDate() != null && filter.toDate() != null) {
            return jpaRepository.findByDoctorIdAndAppointmentDateBetween(
                    filter.doctorId(), filter.fromDate(), filter.toDate()
            ).stream();
        }
        if (filter.facilityId() != null && filter.fromDate() != null && filter.toDate() != null) {
            return jpaRepository.findByFacilityIdAndAppointmentDateBetween(
                    filter.facilityId(), filter.fromDate(), filter.toDate()
            ).stream();
        }
        if (filter.fromDate() != null && filter.toDate() != null) {
            return jpaRepository.findByAppointmentDateBetween(filter.fromDate(), filter.toDate()).stream();
        }
        if (filter.doctorId() != null && !filter.doctorId().isBlank()) {
            return jpaRepository.findByDoctorId(filter.doctorId()).stream();
        }
        if (filter.facilityId() != null) {
            return jpaRepository.findByFacilityId(filter.facilityId()).stream();
        }
        if (filter.patientId() != null) {
            return jpaRepository.findByPatientId(filter.patientId()).stream();
        }
        if (filter.status() != null) {
            return jpaRepository.findByStatus(filter.status()).stream();
        }
        return jpaRepository.findAll().stream();
    }

    @Override
    public void saveAll(List<Appointment> appointments) {
        jpaRepository.saveAll(appointments);
    }

    @Override
    public long countByStatus(AppointmentStatus status) {
        return jpaRepository.countByStatus(status);
    }

    @Override
    public long countByAppointmentDate(LocalDate date) {
        return jpaRepository.countByAppointmentDate(date);
    }

    @Override
    public long countByAppointmentDateAndStatus(LocalDate date, AppointmentStatus status) {
        return jpaRepository.countByAppointmentDateAndStatus(date, status);
    }

    @Override
    public long countByAppointmentDateBetween(LocalDate fromDate, LocalDate toDate) {
        return jpaRepository.countByAppointmentDateBetween(fromDate, toDate);
    }

    @Override
    public long countByAppointmentDateBetweenAndStatus(LocalDate fromDate, LocalDate toDate, AppointmentStatus status) {
        return jpaRepository.countByAppointmentDateBetweenAndStatus(fromDate, toDate, status);
    }
}
