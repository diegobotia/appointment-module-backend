package com.ipscentir.appointments.application.service;

import com.ipscentir.appointments.application.dto.AppointmentDTO;
import com.ipscentir.appointments.application.mapper.AppointmentMapper;
import com.ipscentir.appointments.domain.model.appointment.Appointment;
import com.ipscentir.appointments.infrastructure.persistence.jpa.PacienteRepository;
import com.ipscentir.appointments.infrastructure.persistence.jpa.entity.Paciente;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AppointmentEnrichmentService {

    private final AppointmentMapper appointmentMapper;
    private final MedicoLookupService medicoLookupService;
    private final PacienteRepository pacienteRepository;

    public AppointmentDTO toDto(Appointment appointment) {
        return enrichSingle(appointmentMapper.toDto(appointment), appointment);
    }

    public List<AppointmentDTO> toDtos(List<Appointment> appointments) {
        if (appointments.isEmpty()) {
            return List.of();
        }
        List<AppointmentDTO> base = appointments.stream().map(appointmentMapper::toDto).toList();
        Map<String, String> medicoNames = resolveMedicoNames(appointments);
        Map<UUID, String> patientNames = resolvePatientNames(appointments);
        return base.stream()
                .map(dto -> withDisplayNames(dto, medicoNames, patientNames))
                .toList();
    }

    private AppointmentDTO enrichSingle(AppointmentDTO base, Appointment appointment) {
        Map<String, String> medicoNames = resolveMedicoNames(List.of(appointment));
        Map<UUID, String> patientNames = resolvePatientNames(List.of(appointment));
        return withDisplayNames(base, medicoNames, patientNames);
    }

    private AppointmentDTO withDisplayNames(
            AppointmentDTO dto,
            Map<String, String> medicoNames,
            Map<UUID, String> patientNames
    ) {
        String patientDisplayName = dto.patientId() != null
                ? patientNames.get(dto.patientId())
                : null;
        return new AppointmentDTO(
                dto.id(),
                dto.patientId(),
                dto.medicoId(),
                dto.sedeId(),
                dto.additionalMedicoIds(),
                dto.scheduleId(),
                dto.appointmentDate(),
                dto.appointmentTime(),
                dto.durationMinutes(),
                dto.appointmentType(),
                dto.status(),
                dto.bookingChannel(),
                dto.n8nConversationId(),
                dto.reason(),
                dto.notes(),
                dto.createdAt(),
                dto.updatedAt(),
                medicoNames.get(dto.medicoId()),
                patientDisplayName,
                dto.administrative()
        );
    }

    private Map<String, String> resolveMedicoNames(List<Appointment> appointments) {
        Set<String> medicoIds = new HashSet<>();
        for (Appointment appointment : appointments) {
            medicoIds.add(appointment.getDoctorId());
            for (String additionalId : appointment.getAdditionalDoctorIds()) {
                medicoIds.add(additionalId);
            }
        }
        return medicoLookupService.resolveDisplayNames(medicoIds);
    }

    private Map<UUID, String> resolvePatientNames(List<Appointment> appointments) {
        Set<UUID> patientIds = appointments.stream()
                .map(Appointment::getPatientId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(HashSet::new));
        if (patientIds.isEmpty()) {
            return Map.of();
        }
        return pacienteRepository.findAllById(patientIds).stream()
                .collect(Collectors.toMap(
                        Paciente::getId,
                        AppointmentEnrichmentService::formatPatientName,
                        (left, right) -> left
                ));
    }

    private static String formatPatientName(Paciente paciente) {
        return (paciente.getNombres() + " " + paciente.getApellidos()).trim();
    }
}
