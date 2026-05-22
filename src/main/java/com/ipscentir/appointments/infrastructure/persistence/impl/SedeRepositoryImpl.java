package com.ipscentir.appointments.infrastructure.persistence.impl;

import com.ipscentir.appointments.domain.model.sede.Sede;
import com.ipscentir.appointments.domain.repository.SedeRepository;
import com.ipscentir.appointments.infrastructure.persistence.jpa.SedeCodeAliasJpaRepository;
import com.ipscentir.appointments.infrastructure.persistence.jpa.SedeJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class SedeRepositoryImpl implements SedeRepository {

    private final SedeJpaRepository sedeJpaRepository;
    private final SedeCodeAliasJpaRepository sedeCodeAliasJpaRepository;

    @Override
    public List<Sede> findAllOrderById() {
        return sedeJpaRepository.findAllByOrderByIdAsc();
    }

    @Override
    public Optional<Sede> findById(Integer id) {
        return sedeJpaRepository.findById(id);
    }

    @Override
    public Optional<Sede> findByCodeOrAlias(String codeOrAlias) {
        return sedeCodeAliasJpaRepository.findByAliasCode(codeOrAlias)
                .flatMap(alias -> sedeJpaRepository.findById(alias.getSedeId()));
    }
}
