package com.ipscentir.appointments.application.dto.form;

import java.util.Map;
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
        List<CatalogOptionDTO> countries,
        List<CatalogOptionDTO> municipalities,
        List<CatalogOptionDTO> territorialZones,
        Map<String, List<CatalogOptionDTO>> catalogs
) {
    public PatientRegistrationFormConfigResponse(
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
        this(
                formBaseUrl,
                submitPath,
                statusPath,
                supportedDocumentTypes,
                urlTemplate,
                genders,
                civilStatus,
                occupations,
                bloodGroups,
                schoolingLevels,
                countries,
                List.of(),
                                List.of(),
                                Map.of()
        );
    }

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
                                List.of(),
                                List.of(),
                                List.of(),
                                Map.of()
        );
    }
}
