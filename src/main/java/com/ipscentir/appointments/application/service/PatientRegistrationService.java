package com.ipscentir.appointments.application.service;

import com.ipscentir.appointments.application.dto.form.CatalogOptionDTO;
import com.ipscentir.appointments.application.dto.form.CreatePatientRegistrationRequest;
import com.ipscentir.appointments.application.dto.form.DocumentTypeOptionDTO;
import com.ipscentir.appointments.application.dto.form.PatientRegistrationFormConfigResponse;
import com.ipscentir.appointments.application.dto.form.PatientRegistrationResponse;
import com.ipscentir.appointments.application.dto.form.PatientRegistrationStatusResponse;
import com.ipscentir.appointments.application.exception.PatientAlreadyExistsException;
import com.ipscentir.appointments.domain.model.catalog.ColombianIdentificationType;
import com.ipscentir.appointments.infrastructure.persistence.jpa.ContactoRepository;
import com.ipscentir.appointments.infrastructure.persistence.jpa.DireccionRepository;
import com.ipscentir.appointments.infrastructure.persistence.jpa.PacienteRepository;
import com.ipscentir.appointments.infrastructure.persistence.jpa.entity.Contacto;
import com.ipscentir.appointments.infrastructure.persistence.jpa.entity.Direccion;
import com.ipscentir.appointments.infrastructure.persistence.jpa.entity.Paciente;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PatientRegistrationService {

    private final ContactoRepository contactoRepository;
    private final DireccionRepository direccionRepository;
    private final PacienteRepository pacienteRepository;
    private final TipoIdentificacionResolver tipoIdentificacionResolver;
    private final JdbcTemplate jdbcTemplate;

    @Value("${patient-registration.form-base-url:https://citas.ipscentir.com/registro}")
    private String formBaseUrl;

    public PatientRegistrationFormConfigResponse getFormConfig() {
        List<DocumentTypeOptionDTO> documentTypes = loadDocumentTypes();
        List<CatalogOptionDTO> genders = loadCatalog("core.generos");
        List<CatalogOptionDTO> civilStatus = loadCatalog("core.estados_civil");
        List<CatalogOptionDTO> occupations = loadCatalog("core.ocupaciones");
        List<CatalogOptionDTO> bloodGroups = loadCatalog("core.grupos_sanguineos");
        List<CatalogOptionDTO> schoolingLevels = loadCatalog("core.escolaridades");
        List<CatalogOptionDTO> countries = loadCatalog("core.paises");
        List<CatalogOptionDTO> municipalities = loadCatalog("core.municipio", "cod", "nombre");
        List<CatalogOptionDTO> territorialZones = loadCatalog("core.zonas_territoriales", "cod", "descripcion");

        Map<String, List<CatalogOptionDTO>> catalogs = new LinkedHashMap<>();
        catalogs.put("genders", genders);
        catalogs.put("civilStatus", civilStatus);
        catalogs.put("occupations", occupations);
        catalogs.put("bloodGroups", bloodGroups);
        catalogs.put("schoolingLevels", schoolingLevels);
        catalogs.put("countries", countries);
        catalogs.put("municipalities", municipalities);
        catalogs.put("territorialZones", territorialZones);

        return new PatientRegistrationFormConfigResponse(
                formBaseUrl,
                "/api/v1/forms/patients",
                "/api/v1/forms/patients/status",
                documentTypes,
                formBaseUrl + "?codTipoIdentificacion={codTipoIdentificacion}&numIdentificacion={numIdentificacion}",
                genders,
                civilStatus,
                occupations,
                bloodGroups,
                schoolingLevels,
                countries,
                municipalities,
                territorialZones,
                catalogs
        );
    }

    private List<CatalogOptionDTO> loadCatalog(String tableName) {
        return loadCatalog(tableName, "id", null);
    }

    private List<CatalogOptionDTO> loadCatalog(String tableName, String idColumn, String labelColumn) {
        try {
            String schema = "public";
            String table = tableName;
            if (tableName.contains(".")) {
                String[] parts = tableName.split("\\.", 2);
                schema = parts[0];
                table = parts[1];
            }

            String resolvedLabelColumn = labelColumn != null
                    ? labelColumn
                    : resolveLabelColumn(schema, table).orElse("id");
            String query = "SELECT CAST(" + idColumn + " AS text), " + resolvedLabelColumn + " FROM " + tableName + " ORDER BY 2";
            return jdbcTemplate.query(query, (rs, rowNum) -> new CatalogOptionDTO(
                    rs.getString(1),
                    rs.getString(2) != null ? rs.getString(2) : rs.getString(1)
            ));
        } catch (Exception ex) {
            log.warn("No se pudo cargar el catálogo {}: {}", tableName, ex.getMessage());
            return List.of();
        }
    }

    private List<DocumentTypeOptionDTO> loadDocumentTypes() {
        try {
            return jdbcTemplate.query(
                    "SELECT codigo, descripcion FROM core.tipo_identificacion ORDER BY descripcion",
                    (rs, rowNum) -> new DocumentTypeOptionDTO(
                            rs.getString(1),
                            rs.getString(2) != null ? rs.getString(2) : rs.getString(1)
                    )
            );
        } catch (Exception ex) {
            log.warn("No se pudo cargar core.tipo_identificacion: {}", ex.getMessage());
        }

        return Arrays.stream(ColombianIdentificationType.values())
                .map(type -> new DocumentTypeOptionDTO(type.getCodigo(), type.getDescripcion()))
                .toList();
    }

    private Optional<String> resolveLabelColumn(String schema, String table) {
        List<String> candidates = List.of("nombre", "descripcion", "descripcion_corta", "name", "label");
        try {
            List<String> existing = jdbcTemplate.queryForList(
                    "SELECT column_name FROM information_schema.columns WHERE table_schema = ? AND table_name = ?",
                    String.class, schema, table);
            return candidates.stream()
                    .filter(existing::contains)
                    .findFirst();
        } catch (Exception ex) {
            log.warn("No se pudo resolver columna para {}.{}: {}", schema, table, ex.getMessage());
            return Optional.empty();
        }
    }

    public PatientRegistrationStatusResponse getRegistrationStatus(String codTipoIdentificacion, String numIdentificacion) {
        String codigo = tipoIdentificacionResolver.resolveCodigo(codTipoIdentificacion);
        return tipoIdentificacionResolver
                .findPaciente(codTipoIdentificacion, numIdentificacion)
                .map(paciente -> new PatientRegistrationStatusResponse(
                        true,
                        paciente.getId(),
                        paciente.getCodTipoIdentificacion(),
                        paciente.getNumIdentificacion(),
                        paciente.getNombres(),
                        paciente.getApellidos()
                ))
                .orElseGet(() -> PatientRegistrationStatusResponse.notRegistered(codigo, numIdentificacion));
    }

    @Transactional
    public PatientRegistrationResponse registerPatient(CreatePatientRegistrationRequest request) {
        String codigo = tipoIdentificacionResolver.canonicalCodigoForStorage(request.getCodTipoIdentificacion());
        request.setCodTipoIdentificacion(codigo);

        if (tipoIdentificacionResolver.existsPaciente(request.getCodTipoIdentificacion(), request.getNumIdentificacion())) {
            throw new PatientAlreadyExistsException(
                    codigo,
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
