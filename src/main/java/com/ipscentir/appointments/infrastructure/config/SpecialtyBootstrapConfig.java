package com.ipscentir.appointments.infrastructure.config;

import com.ipscentir.appointments.domain.model.catalog.SpecialtyCatalog;
import com.ipscentir.appointments.domain.model.specialty.Specialty;
import com.ipscentir.appointments.infrastructure.persistence.jpa.SpecialtyJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class SpecialtyBootstrapConfig {

    @Bean
    CommandLineRunner bootstrapSpecialties(SpecialtyJpaRepository specialtyJpaRepository) {
        return args -> {
            for (SpecialtyCatalog catalog : SpecialtyCatalog.values()) {
                specialtyJpaRepository.findByCode(catalog.name())
                        .orElseGet(() -> specialtyJpaRepository.save(
                                Specialty.builder()
                                        .code(catalog.name())
                                        .displayName(catalog.getDisplayName())
                                        .active(true)
                                        .build()
                        ));
            }

            log.info("Specialty bootstrap completed");
        };
    }
}