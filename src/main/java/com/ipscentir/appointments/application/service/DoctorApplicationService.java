package com.ipscentir.appointments.application.service;

import com.ipscentir.appointments.application.dto.availability.DoctorAvailabilityResponse;
import com.ipscentir.appointments.application.dto.availability.DoctorAvailabilitySlotDTO;
import com.ipscentir.appointments.application.dto.availability.DoctorDayAvailabilityDTO;
import com.ipscentir.appointments.application.security.FacilityAuthorizationService;
import com.ipscentir.appointments.application.security.StaffSecurityHelper;
import com.ipscentir.appointments.application.service.dto.DoctorAvailableDTO;
import com.ipscentir.appointments.domain.model.schedule.AvailableSlotDetail;
import com.ipscentir.appointments.domain.model.security.RoleName;
import com.ipscentir.appointments.domain.model.specialist.Specialist;
import com.ipscentir.appointments.domain.service.AvailabilityService;
import com.ipscentir.appointments.infrastructure.persistence.jpa.SpecialistJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DoctorApplicationService {

    private final SpecialistJpaRepository specialistJpaRepository;
    private final AvailabilityService availabilityService;
    private final FacilityAuthorizationService facilityAuthorizationService;
    private final StaffSecurityHelper staffSecurityHelper;

    @Transactional(readOnly = true)
    public List<DoctorAvailableDTO> findAvailableDoctors(String specialty, UUID facilityId, LocalDate availabilityDate) {
        log.debug("findAvailableDoctors specialty={}, facilityId={}, date={}", specialty, facilityId, availabilityDate);

        List<DoctorAvailableDTO> result = new ArrayList<>();
        List<Specialist> specialists = specialistJpaRepository.findAllByActiveTrue();

        for (Specialist specialist : specialists) {
            if (specialty != null && !specialty.trim().isEmpty()) {
                String spec = specialist.getSpecialty();
                if (spec == null || !spec.equalsIgnoreCase(specialty.trim())) {
                    continue;
                }
            }

            if (facilityId != null && availabilityDate != null) {
                List<AvailableSlotDetail> slots = availabilityService.getAvailableSlotsInRange(
                        specialist.getId(),
                        facilityId,
                        availabilityDate,
                        availabilityDate
                );
                if (slots.isEmpty()) {
                    continue;
                }
            }

            List<String> specs = specialist.getSpecialty() != null
                    ? List.of(specialist.getSpecialty())
                    : List.of();

            result.add(DoctorAvailableDTO.builder()
                    .doctorId(specialist.getId())
                    .name(specialist.getFirstName() + " " + specialist.getLastName())
                    .specialties(specs)
                    .build());
        }

        return result;
    }

    @Transactional(readOnly = true)
    public DoctorAvailabilityResponse getDoctorAvailability(
            String doctorId,
            UUID facilityId,
            LocalDate fromDate,
            LocalDate toDate
    ) {
        specialistJpaRepository.findById(doctorId)
                .orElseThrow(() -> new IllegalArgumentException("Doctor not found"));

        facilityAuthorizationService.assertCurrentUserCanAccessFacility(facilityId);

        if (staffSecurityHelper.hasRole(RoleName.MEDICO)) {
            String ownDoctorId = staffSecurityHelper.requireDoctorIdForMedico();
            if (!ownDoctorId.equals(doctorId)) {
                throw new AccessDeniedException("El médico solo puede consultar su propia disponibilidad");
            }
        }

        LocalDate from = fromDate != null ? fromDate : LocalDate.now();
        LocalDate to = toDate != null ? toDate : from.plusDays(6);

        List<AvailableSlotDetail> slots = availabilityService.getAvailableSlotsInRange(doctorId, facilityId, from, to);

        Map<LocalDate, List<DoctorAvailabilitySlotDTO>> byDay = slots.stream()
                .collect(Collectors.groupingBy(
                        AvailableSlotDetail::appointmentDate,
                        LinkedHashMap::new,
                        Collectors.mapping(
                                s -> new DoctorAvailabilitySlotDTO(
                                        s.appointmentTime(),
                                        s.durationMinutes(),
                                        s.availableSeats()
                                ),
                                Collectors.toList()
                        )
                ));

        List<DoctorDayAvailabilityDTO> days = new ArrayList<>();
        for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
            days.add(new DoctorDayAvailabilityDTO(date, byDay.getOrDefault(date, List.of())));
        }

        return new DoctorAvailabilityResponse(
                doctorId,
                facilityId,
                from,
                to,
                slots.size(),
                days
        );
    }
}
