package com.ipscentir.appointments.application.service;

import com.ipscentir.appointments.application.dto.CatalogItemDTO;
import com.ipscentir.appointments.domain.model.catalog.AppointmentServiceType;
import com.ipscentir.appointments.domain.model.catalog.SpecialtyCatalog;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
public class CatalogService {

    private static final int CURRENT_CATALOG_VERSION = 1;

    public List<CatalogItemDTO> listAppointmentServiceTypes() {
        return Arrays.stream(AppointmentServiceType.values())
                .map(value -> new CatalogItemDTO(value.name(), value.getDisplayName(), CURRENT_CATALOG_VERSION))
                .toList();
    }

    public List<CatalogItemDTO> listSpecialties() {
        return Arrays.stream(SpecialtyCatalog.values())
                .map(value -> new CatalogItemDTO(value.name(), value.getDisplayName(), CURRENT_CATALOG_VERSION))
                .toList();
    }
}
