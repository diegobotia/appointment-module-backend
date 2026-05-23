package com.ipscentir.appointments.domain.repository;

import com.ipscentir.appointments.domain.model.sede.Sede;

import java.util.List;
import java.util.Optional;

public interface SedeRepository {

    List<Sede> findAllOrderById();

    Optional<Sede> findById(Integer id);

    Optional<Sede> findByCodeOrAlias(String codeOrAlias);
}
