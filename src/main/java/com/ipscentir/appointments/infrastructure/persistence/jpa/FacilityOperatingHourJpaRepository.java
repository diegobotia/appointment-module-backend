package com.ipscentir.appointments.infrastructure.persistence.jpa;

import com.ipscentir.appointments.domain.model.facility.FacilityOperatingHour;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FacilityOperatingHourJpaRepository extends JpaRepository<FacilityOperatingHour, UUID> {

    List<FacilityOperatingHour> findBySedeIdOrderByDayOfWeekAsc(Integer sedeId);

    long countBySedeId(Integer sedeId);
}
