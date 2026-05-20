package com.ipscentir.appointments.application.dto.form;

import java.util.List;

public record PatientRegistrationFormConfigResponse(
        String formBaseUrl,
        String submitPath,
        String statusPath,
        List<String> supportedDocumentTypes,
        String urlTemplate
) {
}
