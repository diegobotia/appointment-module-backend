package com.ipscentir.appointments.infrastructure.persistence.impl;

import com.ipscentir.appointments.domain.model.facility.FacilityOperatingHour;
import com.ipscentir.appointments.domain.repository.FacilityOperatingHoursRepository;
import com.ipscentir.appointments.infrastructure.persistence.jpa.FacilityOperatingHourJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class FacilityOperatingHoursRepositoryImpl implements FacilityOperatingHoursRepository {

    private final FacilityOperatingHourJpaRepository jpaRepository;

    @Override
    public List<FacilityOperatingHour> findBySedeIdOrderByDayOfWeek(Integer sedeId) {
        return jpaRepository.findBySedeIdOrderByDayOfWeekAsc(sedeId);
    }

    @Override
    public long countBySedeId(Integer sedeId) {
        return jpaRepository.countBySedeId(sedeId);
    }

    @Override
    public List<FacilityOperatingHour> saveAll(Iterable<FacilityOperatingHour> hours) {
        return jpaRepository.saveAll(hours);
    }
}
