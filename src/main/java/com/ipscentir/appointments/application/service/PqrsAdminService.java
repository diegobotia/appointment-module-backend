package com.ipscentir.appointments.application.service;

import com.ipscentir.appointments.application.dto.PqrsDTO;
import com.ipscentir.appointments.application.dto.admin.PqrsPageResponse;
import com.ipscentir.appointments.application.dto.admin.UpdatePqrsStatusRequest;
import com.ipscentir.appointments.application.mapper.PqrsMapper;
import com.ipscentir.appointments.domain.model.pqrs.Pqrs;
import com.ipscentir.appointments.domain.model.pqrs.PqrsStatus;
import com.ipscentir.appointments.domain.model.pqrs.PqrsType;
import com.ipscentir.appointments.domain.repository.PqrsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PqrsAdminService {

    private final PqrsRepository pqrsRepository;
    private final PqrsMapper pqrsMapper;

    @Transactional(readOnly = true)
    public PqrsPageResponse search(PqrsStatus status, PqrsType tipo, int page, int size) {
        if (page < 0) {
            page = 0;
        }
        if (size <= 0) {
            size = 20;
        }
        if (size > 100) {
            size = 100;
        }

        long total = pqrsRepository.countSearch(status, tipo);
        int totalPages = total == 0 ? 0 : (int) Math.ceil((double) total / size);

        List<PqrsDTO> content = pqrsRepository.search(status, tipo, page * size, size).stream()
                .map(pqrsMapper::toDto)
                .toList();

        return new PqrsPageResponse(content, page, size, total, totalPages);
    }

    @Transactional(readOnly = true)
    public PqrsDTO getById(UUID id) {
        Pqrs pqrs = pqrsRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("PQRS no encontrada"));
        return pqrsMapper.toDto(pqrs);
    }

    @Transactional
    public PqrsDTO updateStatus(UUID id, UpdatePqrsStatusRequest request) {
        Pqrs pqrs = pqrsRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("PQRS no encontrada"));
        pqrs.updateStatus(request.status());
        return pqrsMapper.toDto(pqrsRepository.save(pqrs));
    }
}
