package com.ipscentir.appointments.infrastructure.config;

import com.ipscentir.appointments.domain.model.facility.FacilityMasterData;
import com.ipscentir.appointments.domain.model.sede.Sede;
import com.ipscentir.appointments.domain.model.facility.FacilityOperatingHour;
import com.ipscentir.appointments.domain.model.facility.FacilityResource;
import com.ipscentir.appointments.domain.model.sede.SedeCodeAlias;
import com.ipscentir.appointments.domain.repository.FacilityOperatingHoursRepository;
import com.ipscentir.appointments.domain.repository.FacilityResourceRepository;
import com.ipscentir.appointments.domain.repository.SedeRepository;
import com.ipscentir.appointments.infrastructure.persistence.jpa.SedeCodeAliasJpaRepository;
import com.ipscentir.appointments.infrastructure.persistence.jpa.SedeJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class SedeOperationalDataBootstrap {

    private final SedeRepository sedeRepository;
    private final SedeJpaRepository sedeJpaRepository;
    private final SedeCodeAliasJpaRepository sedeCodeAliasJpaRepository;
    private final FacilityOperatingHoursRepository facilityOperatingHoursRepository;
    private final FacilityResourceRepository facilityResourceRepository;

    @Bean
    CommandLineRunner bootstrapSedeOperationalData() {
        return args -> {
            for (FacilityMasterData.SedeOperationalSeed seed : FacilityMasterData.defaultSedes()) {
                Sede sede = sedeRepository.findById(seed.sedeId()).orElseGet(() -> {
                    Sede created = sedeJpaRepository.save(Sede.builder()
                            .id(seed.sedeId())
                            .nombre(seed.sedeId() == FacilityMasterData.SEDE_ID_CONQUISTADORES
                                    ? "CENTIR DEL SUR S.A.S."
                                    : "CENTRO INTEGRAL DE REHABILITACION")
                            .build());
                    log.info("Sede bootstrap: created core.sede id={}", seed.sedeId());
                    return created;
                });

                {
                    for (String alias : seed.legacyAliases()) {
                        if (sedeCodeAliasJpaRepository.findByAliasCode(alias).isEmpty()) {
                            sedeCodeAliasJpaRepository.save(SedeCodeAlias.builder()
                                    .sedeId(seed.sedeId())
                                    .aliasCode(alias)
                                    .build());
                        }
                    }

                    if (facilityOperatingHoursRepository.countBySedeId(seed.sedeId()) == 0) {
                        facilityOperatingHoursRepository.saveAll(
                                FacilityMasterData.defaultOperatingHours().stream()
                                        .map(h -> FacilityOperatingHour.builder()
                                                .sedeId(seed.sedeId())
                                                .dayOfWeek(h.dayOfWeek())
                                                .openTime(h.openTime())
                                                .closeTime(h.closeTime())
                                                .closed(h.closed())
                                                .build())
                                        .toList()
                        );
                        log.info("Sede bootstrap: operating hours for sede {}", seed.sedeId());
                    }

                    if (facilityResourceRepository.countActiveBySedeId(seed.sedeId()) == 0) {
                        facilityResourceRepository.saveAll(
                                FacilityMasterData.resourcesFor(seed.sedeId()).stream()
                                        .map(r -> FacilityResource.builder()
                                                .sedeId(seed.sedeId())
                                                .resourceType(r.resourceType())
                                                .code(r.code())
                                                .displayName(r.displayName())
                                                .capacityUnits(r.capacityUnits())
                                                .active(true)
                                                .build())
                                        .toList()
                        );
                        log.info("Sede bootstrap: resources for sede {}", seed.sedeId());
                    }
                }
            }
        };
    }
}
