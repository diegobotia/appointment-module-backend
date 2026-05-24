package com.ipscentir.appointments.application.service;

import com.ipscentir.appointments.application.dto.medico.MedicoPageResponse;
import com.ipscentir.appointments.application.dto.medico.MedicoSearchCriteria;
import com.ipscentir.appointments.application.dto.medico.MedicoSummaryDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminMedicoService {

    private final MedicoLookupService medicoLookupService;

    @Transactional(readOnly = true)
    public MedicoPageResponse search(MedicoSearchCriteria criteria) {
        return medicoLookupService.search(criteria);
    }

    @Transactional(readOnly = true)
    public MedicoSummaryDTO getById(String medicoId) {
        return medicoLookupService.getSummaryById(medicoId);
    }
}
