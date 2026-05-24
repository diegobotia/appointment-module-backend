package com.ipscentir.appointments.application.dto.medico;

public record MedicoSearchCriteria(
        String q,
        String numDoc,
        String registro,
        String specialty,
        Boolean active,
        int page,
        int size
) {
}
