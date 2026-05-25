package com.ipscentir.appointments.application.dto.form;

import java.util.List;

public record PatientRegistrationFormConfigResponse(
        String formBaseUrl,
        String submitPath,
        String statusPath,
        List<DocumentTypeOptionDTO> supportedDocumentTypes,
        String urlTemplate,
        List<CatalogOptionDTO> genders,
        List<CatalogOptionDTO> civilStatus,
        List<CatalogOptionDTO> occupations,
        List<CatalogOptionDTO> bloodGroups,
        List<CatalogOptionDTO> schoolingLevels,
        List<CatalogOptionDTO> countries
) {
    public PatientRegistrationFormConfigResponse(
            String formBaseUrl,
            String submitPath,
            String statusPath,
            List<DocumentTypeOptionDTO> supportedDocumentTypes,
            String urlTemplate
    ) {
        this(formBaseUrl,
                submitPath,
                statusPath,
                supportedDocumentTypes,
                urlTemplate,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );
    }
}
