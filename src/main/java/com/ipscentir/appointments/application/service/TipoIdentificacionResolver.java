package com.ipscentir.appointments.application.service;

import com.ipscentir.appointments.domain.model.catalog.ColombianIdentificationType;
import com.ipscentir.appointments.infrastructure.persistence.jpa.PacienteRepository;
import com.ipscentir.appointments.infrastructure.persistence.jpa.entity.Paciente;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class TipoIdentificacionResolver {

    private final PacienteRepository pacienteRepository;

    /**
     * Convierte descripción n8n, código DIAN o alias legacy (CC, TI…) al código canónico para FK.
     */
    public String resolveCodigo(String tipoIdentificacion) {
        return ColombianIdentificationType.resolveCodigo(tipoIdentificacion);
    }

    public List<String> lookupKeys(String tipoIdentificacion) {
        String trimmed = tipoIdentificacion.trim();
        Set<String> keys = new LinkedHashSet<>();
        ColombianIdentificationType.tryResolve(trimmed)
                .ifPresent(type -> keys.addAll(type.lookupCandidates()));
        keys.add(resolveCodigo(trimmed));
        keys.add(trimmed);
        return List.copyOf(keys);
    }

    public Optional<Paciente> findPaciente(String tipoIdentificacion, String numIdentificacion) {
        for (String codTipo : lookupKeys(tipoIdentificacion)) {
            Optional<Paciente> found = pacienteRepository
                    .findByCodTipoIdentificacionAndNumIdentificacion(codTipo, numIdentificacion);
            if (found.isPresent()) {
                return found;
            }
        }
        return Optional.empty();
    }

    public boolean existsPaciente(String tipoIdentificacion, String numIdentificacion) {
        return findPaciente(tipoIdentificacion, numIdentificacion).isPresent();
    }

    /**
     * Código canónico para persistir (siempre DIAN).
     */
    public String canonicalCodigoForStorage(String tipoIdentificacion) {
        return resolveCodigo(tipoIdentificacion);
    }
}
