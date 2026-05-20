package com.ipscentir.appointments.infrastructure.config;

import com.ipscentir.appointments.domain.model.facility.Facility;
import com.ipscentir.appointments.infrastructure.persistence.jpa.FacilityJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class FacilityBootstrapConfig {

    @Bean
    CommandLineRunner bootstrapFacilities(FacilityJpaRepository facilityJpaRepository) {
        return args -> {
            List<Facility> activeFacilities = facilityJpaRepository.findByActiveTrue();
            if (activeFacilities.isEmpty()) {
                facilityJpaRepository.save(Facility.builder()
                        .code("SEDE_PRINCIPAL")
                        .name("Sede Principal")
                        .address("Direccion pendiente - Sede Principal")
                        .active(true)
                        .build());

                facilityJpaRepository.save(Facility.builder()
                        .code("SEDE_NORTE")
                        .name("Sede Norte")
                        .address("Direccion pendiente - Sede Norte")
                        .active(true)
                        .build());
                log.info("Facility bootstrap completed.");
            }
        };
    }
}
