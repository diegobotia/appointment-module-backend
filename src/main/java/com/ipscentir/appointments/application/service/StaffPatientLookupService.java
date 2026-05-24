package com.ipscentir.appointments.application.service;

import com.ipscentir.appointments.application.dto.staff.PatientSummaryDTO;
import com.ipscentir.appointments.application.exception.PatientNotFoundException;
import com.ipscentir.appointments.infrastructure.persistence.jpa.PacienteRepository;
import com.ipscentir.appointments.infrastructure.persistence.jpa.entity.Paciente;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StaffPatientLookupService {

    private final PacienteRepository pacienteRepository;
    private final TipoIdentificacionResolver tipoIdentificacionResolver;

    @Transactional(readOnly = true)
    public PatientSummaryDTO searchByDocument(String codTipoIdentificacion, String numIdentificacion) {
        String num = requireNumIdentificacion(numIdentificacion);
        String codTipo = normalize(codTipoIdentificacion);

        Paciente paciente = findPaciente(codTipo, num)
                .orElseThrow(() -> new PatientNotFoundException(
                        codTipo != null ? codTipo : "documento",
                        num
                ));

        return toSummary(paciente);
    }

    @Transactional(readOnly = true)
    public PatientSummaryDTO getById(UUID patientId) {
        Paciente paciente = pacienteRepository.findById(patientId)
                .orElseThrow(() -> new PatientNotFoundException(patientId));
        return toSummary(paciente);
    }

    private Optional<Paciente> findPaciente(String codTipoIdentificacion, String numIdentificacion) {
        if (codTipoIdentificacion != null) {
            return tipoIdentificacionResolver.findPaciente(codTipoIdentificacion, numIdentificacion);
        }
        return pacienteRepository.findByNumIdentificacion(numIdentificacion);
    }

    private PatientSummaryDTO toSummary(Paciente paciente) {
        String fullName = (paciente.getNombres() + " " + paciente.getApellidos()).trim();
        return new PatientSummaryDTO(
                paciente.getId(),
                paciente.getNombres(),
                paciente.getApellidos(),
                fullName,
                paciente.getCodTipoIdentificacion(),
                tipoIdentificacionResolver.resolveDescripcion(paciente.getCodTipoIdentificacion()),
                paciente.getNumIdentificacion()
        );
    }

    private String requireNumIdentificacion(String numIdentificacion) {
        String normalized = normalize(numIdentificacion);
        if (normalized == null) {
            throw new IllegalArgumentException("numIdentificacion es requerido");
        }
        return normalized;
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
