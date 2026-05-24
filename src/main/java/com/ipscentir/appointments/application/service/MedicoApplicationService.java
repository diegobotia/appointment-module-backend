package com.ipscentir.appointments.application.service;

import com.ipscentir.appointments.application.dto.availability.MedicoAvailabilityResponse;
import com.ipscentir.appointments.application.dto.availability.MedicoAvailabilitySlotDTO;
import com.ipscentir.appointments.application.dto.availability.MedicoDayAvailabilityDTO;
import com.ipscentir.appointments.application.dto.medico.MedicoAvailableDTO;
import com.ipscentir.appointments.application.security.SedeAuthorizationService;
import com.ipscentir.appointments.application.security.StaffSecurityHelper;
import com.ipscentir.appointments.domain.model.schedule.AvailableSlotDetail;
import com.ipscentir.appointments.domain.model.security.RoleName;
import com.ipscentir.appointments.domain.model.specialist.Specialist;
import com.ipscentir.appointments.domain.service.AvailabilityService;
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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MedicoApplicationService {

    private final MedicoLookupService medicoLookupService;
    private final AvailabilityService availabilityService;
    private final SedeAuthorizationService sedeAuthorizationService;
    private final StaffSecurityHelper staffSecurityHelper;

    @Transactional(readOnly = true)
    public List<MedicoAvailableDTO> findAvailableMedicos(String specialty, Integer sedeId, LocalDate availabilityDate) {
        log.debug("findAvailableMedicos specialty={}, sedeId={}, date={}", specialty, sedeId, availabilityDate);

        List<MedicoAvailableDTO> result = new ArrayList<>();
        for (Specialist specialist : medicoLookupService.findAllActive()) {
            if (specialty != null && !specialty.trim().isEmpty()) {
                String spec = specialist.getSpecialty();
                if (spec == null || !spec.equalsIgnoreCase(specialty.trim())) {
                    continue;
                }
            }

            if (sedeId != null && availabilityDate != null) {
                List<AvailableSlotDetail> slots = availabilityService.getAvailableSlotsInRange(
                        specialist.getId(),
                        sedeId,
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

            result.add(MedicoAvailableDTO.builder()
                    .medicoId(specialist.getId())
                    .name(MedicoLookupService.formatFullName(specialist))
                    .specialties(specs)
                    .build());
        }

        return result;
    }

    @Transactional(readOnly = true)
    public MedicoAvailabilityResponse getMedicoAvailability(
            String medicoId,
            Integer sedeId,
            LocalDate fromDate,
            LocalDate toDate
    ) {
        medicoLookupService.requireById(medicoId);
        sedeAuthorizationService.assertCurrentUserCanAccessSede(sedeId);

        if (staffSecurityHelper.hasRole(RoleName.MEDICO)) {
            String ownMedicoId = staffSecurityHelper.requireDoctorIdForMedico();
            if (!ownMedicoId.equals(medicoId)) {
                throw new AccessDeniedException("El médico solo puede consultar su propia disponibilidad");
            }
        }

        LocalDate from = fromDate != null ? fromDate : LocalDate.now();
        LocalDate to = toDate != null ? toDate : from.plusDays(6);

        List<AvailableSlotDetail> slots = availabilityService.getAvailableSlotsInRange(medicoId, sedeId, from, to);

        Map<LocalDate, List<MedicoAvailabilitySlotDTO>> byDay = slots.stream()
                .collect(Collectors.groupingBy(
                        AvailableSlotDetail::appointmentDate,
                        LinkedHashMap::new,
                        Collectors.mapping(
                                s -> new MedicoAvailabilitySlotDTO(
                                        s.appointmentTime(),
                                        s.durationMinutes(),
                                        s.availableSeats()
                                ),
                                Collectors.toList()
                        )
                ));

        List<MedicoDayAvailabilityDTO> days = new ArrayList<>();
        for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
            days.add(new MedicoDayAvailabilityDTO(date, byDay.getOrDefault(date, List.of())));
        }

        return new MedicoAvailabilityResponse(
                medicoId,
                sedeId,
                from,
                to,
                slots.size(),
                days
        );
    }
}
