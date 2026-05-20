package com.ipscentir.appointments.application.service;

import com.ipscentir.appointments.application.service.dto.DoctorAvailableDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import com.ipscentir.appointments.infrastructure.persistence.jpa.SpecialistJpaRepository;
import com.ipscentir.appointments.domain.model.specialist.Specialist;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DoctorApplicationService {

    private final SpecialistJpaRepository specialistJpaRepository;

    /**
     * Método básico que devuelve una lista de médicos disponibles.
     */
    public List<DoctorAvailableDTO> findAvailableDoctors(String specialty, UUID facilityId, LocalDate availabilityDate) {
        log.debug("findAvailableDoctors called with specialty={}, facilityId={}, availabilityDate={}", specialty, facilityId, availabilityDate);

        List<DoctorAvailableDTO> result = new ArrayList<>();

        try {
            List<Specialist> list = specialistJpaRepository.findAllByActiveTrue();
            for (Specialist specialist : list) {
                // Si se especifica especialidad, filtrar por ella (coincidencia parcial/ignorar mayúsculas/minúsculas)
                if (specialty != null && !specialty.trim().isEmpty()) {
                    String spec = specialist.getSpecialty();
                    if (spec == null || !spec.equalsIgnoreCase(specialty.trim())) {
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
        } catch (Exception e) {
            log.warn("Error obteniendo doctors from hc.medicos", e);
        }

        return result;
    }

    public Object getDoctorAvailability(String doctorId, LocalDate startDate) {
        log.debug("getDoctorAvailability called for doctorId={} startDate={}", doctorId, startDate);
        return List.of();
    }
}
