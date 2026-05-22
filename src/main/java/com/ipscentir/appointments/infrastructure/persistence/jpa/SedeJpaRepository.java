package com.ipscentir.appointments.infrastructure.persistence.jpa;

import com.ipscentir.appointments.domain.model.sede.Sede;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SedeJpaRepository extends JpaRepository<Sede, Integer> {

    List<Sede> findAllByOrderByIdAsc();
}
