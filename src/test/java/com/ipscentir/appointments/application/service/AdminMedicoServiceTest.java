package com.ipscentir.appointments.application.service;

import com.ipscentir.appointments.application.dto.medico.MedicoSearchCriteria;
import com.ipscentir.appointments.application.exception.MedicoNotFoundException;
import com.ipscentir.appointments.domain.model.specialist.Specialist;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminMedicoServiceTest {

    @Mock
    private MedicoLookupService medicoLookupService;

    @InjectMocks
    private AdminMedicoService adminMedicoService;

    @Test
    void searchDelegatesToLookupService() {
        var page = new com.ipscentir.appointments.application.dto.medico.MedicoPageResponse(
                List.of(), 0, 20, 0, 0
        );
        var criteria = new MedicoSearchCriteria("  ", "", null, "  ", null, 0, 20);
        when(medicoLookupService.search(criteria)).thenReturn(page);

        assertEquals(page, adminMedicoService.search(criteria));
        verify(medicoLookupService).search(criteria);
    }

    @Test
    void getByIdThrowsWhenMissing() {
        String id = UUID.randomUUID().toString();
        when(medicoLookupService.getSummaryById(id)).thenThrow(new MedicoNotFoundException(id));

        assertThrows(MedicoNotFoundException.class, () -> adminMedicoService.getById(id));
    }
}
