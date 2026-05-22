package com.ipscentir.appointments.infrastructure.persistence.jpa;

import com.ipscentir.appointments.domain.model.sede.SedeCodeAlias;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SedeCodeAliasJpaRepository extends JpaRepository<SedeCodeAlias, UUID> {

    Optional<SedeCodeAlias> findByAliasCode(String aliasCode);
}
