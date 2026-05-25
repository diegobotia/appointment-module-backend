package com.ipscentir.appointments.infrastructure.persistence.jpa;

import com.ipscentir.appointments.domain.model.specialist.Specialist;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SpecialistJpaRepositoryCustom {
    Page<Specialist> search(String q, String numDoc, String registro, String specialty, Boolean active, Pageable pageable);
}
