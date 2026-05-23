package com.ipscentir.appointments.domain.repository;

import com.ipscentir.appointments.domain.model.facility.FacilityOperatingHour;

import java.util.List;
import java.util.UUID;

public interface FacilityOperatingHoursRepository {

    List<FacilityOperatingHour> findBySedeIdOrderByDayOfWeek(Integer sedeId);

    long countBySedeId(Integer sedeId);

    List<FacilityOperatingHour> saveAll(Iterable<FacilityOperatingHour> hours);
}
