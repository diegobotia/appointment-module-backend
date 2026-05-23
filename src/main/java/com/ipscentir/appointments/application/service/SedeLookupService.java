package com.ipscentir.appointments.application.service;

import com.ipscentir.appointments.application.exception.SedeNotFoundException;
import com.ipscentir.appointments.domain.model.sede.Sede;
import com.ipscentir.appointments.domain.repository.SedeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SedeLookupService {

    private final SedeRepository sedeRepository;

    @Transactional(readOnly = true)
    public Sede requireByCodeOrAlias(String codeOrAlias) {
        return sedeRepository.findByCodeOrAlias(codeOrAlias)
                .orElseThrow(() -> new SedeNotFoundException(codeOrAlias));
    }

    @Transactional(readOnly = true)
    public Sede requireById(Integer sedeId) {
        return sedeRepository.findById(sedeId)
                .orElseThrow(() -> new SedeNotFoundException(sedeId));
    }
}
