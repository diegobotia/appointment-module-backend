package com.ipscentir.appointments.application.service;

import com.ipscentir.appointments.application.dto.form.CreatePatientRegistrationRequest;
import com.ipscentir.appointments.application.dto.form.PatientRegistrationFormConfigResponse;
import com.ipscentir.appointments.application.dto.form.PatientRegistrationResponse;
import com.ipscentir.appointments.application.dto.form.PatientRegistrationStatusResponse;
import com.ipscentir.appointments.application.exception.PatientAlreadyExistsException;
import com.ipscentir.appointments.infrastructure.persistence.jpa.ContactoRepository;
import com.ipscentir.appointments.infrastructure.persistence.jpa.DireccionRepository;
import com.ipscentir.appointments.infrastructure.persistence.jpa.PacienteRepository;
import com.ipscentir.appointments.infrastructure.persistence.jpa.entity.Contacto;
import com.ipscentir.appointments.infrastructure.persistence.jpa.entity.Direccion;
import com.ipscentir.appointments.infrastructure.persistence.jpa.entity.Paciente;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PatientRegistrationService {

    private final ContactoRepository contactoRepository;
    private final DireccionRepository direccionRepository;
    private final PacienteRepository pacienteRepository;

    @Value("${patient-registration.form-base-url:https://citas.ipscentir.com/registro}")
    private String formBaseUrl;

    public PatientRegistrationFormConfigResponse getFormConfig() {
        return new PatientRegistrationFormConfigResponse(
                formBaseUrl,
                "/api/v1/forms/patients",
                "/api/v1/forms/patients/status",
                List.of("CC", "TI", "CE", "PA", "RC"),
                formBaseUrl + "?codTipoIdentificacion={codTipoIdentificacion}&numIdentificacion={numIdentificacion}"
        );
    }

    public PatientRegistrationStatusResponse getRegistrationStatus(String codTipoIdentificacion, String numIdentificacion) {
        return pacienteRepository
                .findByCodTipoIdentificacionAndNumIdentificacion(codTipoIdentificacion, numIdentificacion)
                .map(paciente -> new PatientRegistrationStatusResponse(
                        true,
                        paciente.getId(),
                        paciente.getCodTipoIdentificacion(),
                        paciente.getNumIdentificacion(),
                        paciente.getNombres(),
                        paciente.getApellidos()
                ))
                .orElseGet(() -> PatientRegistrationStatusResponse.notRegistered(codTipoIdentificacion, numIdentificacion));
    }

    @Transactional
    public PatientRegistrationResponse registerPatient(CreatePatientRegistrationRequest request) {
        if (pacienteRepository.existsByCodTipoIdentificacionAndNumIdentificacion(
                request.getCodTipoIdentificacion(),
                request.getNumIdentificacion()
        )) {
            throw new PatientAlreadyExistsException(
                    request.getCodTipoIdentificacion(),
                    request.getNumIdentificacion()
            );
        }

        UUID contactId = createContact(request.getTelefono(), request.getEmail());
        UUID directionId = createDirection(request);
        Paciente paciente = createPatient(request, contactId, directionId);

        log.info(
                "Paciente registrado vía formulario: {} {} ({}{})",
                paciente.getNombres(),
                paciente.getApellidos(),
                paciente.getCodTipoIdentificacion(),
                paciente.getNumIdentificacion()
        );

        return new PatientRegistrationResponse(
                paciente.getId(),
                paciente.getCodTipoIdentificacion(),
                paciente.getNumIdentificacion(),
                paciente.getNombres(),
                paciente.getApellidos(),
                request.getEmail(),
                "Paciente registrado exitosamente",
                LocalDateTime.now()
        );
    }

    private UUID createContact(String telefono, String email) {
        Contacto contacto = Contacto.builder()
                .email(email)
                .telefono(telefono)
                .build();
        return contactoRepository.save(contacto).getId();
    }

    private UUID createDirection(CreatePatientRegistrationRequest request) {
        Direccion direccion = Direccion.builder()
                .codMunicipio(request.getCodMunicipio())
                .codZonaTerritorial(request.getCodZonaTerritorial())
                .detalle(request.getDireccionDetalle())
                .barrio(request.getBarrio())
                .build();
        return direccionRepository.save(direccion).getId();
    }

    private Paciente createPatient(CreatePatientRegistrationRequest request, UUID contactId, UUID directionId) {
        Paciente paciente = Paciente.builder()
                .nombres(request.getNombres())
                .apellidos(request.getApellidos())
                .numIdentificacion(request.getNumIdentificacion())
                .codTipoIdentificacion(request.getCodTipoIdentificacion())
                .fechaNacimiento(request.getFechaNacimiento())
                .idGenero(UUID.fromString(request.getIdGenero()))
                .idEstadoCivil(UUID.fromString(request.getIdEstadoCivil()))
                .idOcupacion(UUID.fromString(request.getIdOcupacion()))
                .idGrupoSanguineo(UUID.fromString(request.getIdGrupoSanguineo()))
                .idEscolaridad(UUID.fromString(request.getIdEscolaridad()))
                .estrato(request.getEstrato().shortValue())
                .idPaisOrigen(request.getIdPaisOrigen())
                .idDireccion(directionId)
                .idContacto(contactId)
                .build();
        return pacienteRepository.save(paciente);
    }
}
