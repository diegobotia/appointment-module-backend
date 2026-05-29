package com.ipscentir.appointments.infrastructure.persistence.impl;

import com.ipscentir.appointments.domain.model.appointment.Appointment;
import com.ipscentir.appointments.domain.model.appointment.AppointmentStatus;
import com.ipscentir.appointments.domain.model.appointment.AppointmentType;
import com.ipscentir.appointments.domain.model.appointment.BookingChannel;
import com.ipscentir.appointments.domain.repository.AppointmentRepository;
import com.ipscentir.appointments.infrastructure.persistence.jpa.AppointmentJpaRepository;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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
    public boolean existsByPatientIdAndDateExcluding(UUID patientId, LocalDate date, UUID excludeAppointmentId) {
        return jpaRepository.existsByPatientIdAndAppointmentDateAndActiveStatusExcluding(
                patientId,
                date,
                excludeAppointmentId
        );
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
        Specification<Appointment> spec = buildSpecification(filter);
        Sort sort = Sort.by(Sort.Direction.DESC, "appointmentDate", "appointmentTime");
        return jpaRepository.findAll(spec, sort);
    }

    private Specification<Appointment> buildSpecification(AppointmentSearchFilter filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (filter.sedeId() != null) {
                predicates.add(cb.equal(root.get("sedeId"), filter.sedeId()));
            }
            if (filter.medicoId() != null && !filter.medicoId().isBlank()) {
                Join<Object, Object> participants = root.join("participants");
                predicates.add(cb.or(
                        cb.equal(root.get("doctorId"), filter.medicoId()),
                        cb.equal(participants.get("doctorId"), filter.medicoId())
                ));
                query.distinct(true);
            }
            if (filter.patientId() != null) {
                predicates.add(cb.equal(root.get("patientId"), filter.patientId()));
            }
            if (filter.status() != null) {
                predicates.add(cb.equal(root.get("status"), filter.status()));
            }
            if (filter.bookingChannel() != null) {
                predicates.add(cb.equal(root.get("bookingChannel"), filter.bookingChannel()));
            }
            if (filter.fromDate() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("appointmentDate"), filter.fromDate()));
            }
            if (filter.toDate() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("appointmentDate"), filter.toDate()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
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

    @Override
    public long countByBookingChannel(BookingChannel bookingChannel) {
        return jpaRepository.countByBookingChannel(bookingChannel);
    }
}
