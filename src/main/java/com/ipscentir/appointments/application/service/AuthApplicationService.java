package com.ipscentir.appointments.application.service;

import com.ipscentir.appointments.application.service.dto.RegisterPatientRequest;
import com.ipscentir.appointments.application.service.dto.RegisterPatientResponse;
import com.ipscentir.appointments.infrastructure.persistence.jpa.*;
import com.ipscentir.appointments.infrastructure.persistence.jpa.entity.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * AuthApplicationService
 * 
 * Servicio de aplicación que orquesta la autenticación y registro.
 * 
 * Responsabilidades:
 * - Registrar nuevos pacientes
 * - Validar datos de entrada
 * - Crear registros en core.pacientes y core.profiles
 * - Coordinar con Supabase Auth
 * - Devolver tokens o confirmaciones de registro
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthApplicationService {

    private final ContactoRepository contactoRepository;
    private final DireccionRepository direccionRepository;
    private final PacienteRepository pacienteRepository;
    private final ProfileRepository profileRepository;
    private final RoleRepository roleRepository;
    private final SpecialistMetadataRepository specialistMetadataRepository;

    @Value("${supabase.url:https://project.supabase.co}")
    private String supabaseUrl;

    @Value("${supabase.api-key:public-anon-key}")
    private String supabaseAnonKey;

    /**
     * Registra un nuevo paciente en el sistema.
     * 
     * Flujo:
     * 1. Valida que el email no esté duplicado en core.profiles
     * 2. Crea un perfil en core.profiles
     * 3. Crea un registro en core.pacientes
     * 4. Crea datos de contacto en core.contacto
     * 5. Crea datos de dirección en core.direccion
     * 6. Asigna rol de "Paciente"
     * 7. Devuelve confirmación con los IDs generados
     * 
     * Nota: La creación en auth.users será delegada a Supabase Auth
     * mediante una llamada posterior desde el frontend con signUp()
     * 
     * @param request datos del paciente
     * @return respuesta con IDs creados
     */
    @Transactional
    public RegisterPatientResponse registerPatient(RegisterPatientRequest request) {
        log.info("Iniciando registro de paciente: {}", request.getEmail());

        try {
            // TODO: Paso 1 - Validar email no duplicado
            // validateEmailNotExists(request.getEmail());

            // TODO: Paso 2 - Crear datos de contacto
            UUID contactId = createContact(request.getTelefono(), request.getEmail());

            // TODO: Paso 3 - Crear datos de dirección
            UUID directionId = createDirection(
                request.getCodMunicipio(),
                request.getCodZonaTerritorial(),
                request.getDireccionDetalle(),
                request.getBarrio()
            );

            // TODO: Paso 4 - Crear paciente en core.pacientes
            UUID patientId = createPatient(
                request,
                contactId,
                directionId
            );

            // TODO: Paso 5 - Crear perfil en core.profiles
            // (Sin auth.users aún - eso lo hace Supabase Auth después)
            UUID profileId = createProfile(request.getEmail(), request.getNombres(), request.getApellidos());

            // TODO: Paso 6 - Asignar rol de "Paciente"
            assignPatientRole(profileId);

            log.info("Paciente registrado exitosamente: {} (ID: {})", request.getEmail(), patientId);

            return RegisterPatientResponse.builder()
                .patientId(patientId.toString())
                .profileId(profileId.toString())
                .email(request.getEmail())
                .nombres(request.getNombres())
                .apellidos(request.getApellidos())
                .message("Paciente registrado exitosamente")
                .createdAt(LocalDateTime.now())
                .nextSteps("El paciente puede ahora completar la autenticación en Supabase Auth y agendar citas")
                .build();

        } catch (Exception e) {
            log.error("Error al registrar paciente: {}", request.getEmail(), e);
            throw new RuntimeException("Error al registrar paciente: " + e.getMessage(), e);
        }
    }

    /**
     * Crea un registro de contacto (email y teléfono)
     */
    private UUID createContact(String telefono, String email) {
        log.debug("Creando contacto para email: {}", email);

        Contacto contacto = Contacto.builder()
            .email(email)
            .telefono(telefono)
            .build();

        Contacto saved = contactoRepository.save(contacto);
        return saved.getId();
    }

    /**
     * Crea un registro de dirección
     */
    private UUID createDirection(String codMunicipio, String codZona, String detalle, String barrio) {
        log.debug("Creando dirección en municipio: {}", codMunicipio);

        Direccion direccion = Direccion.builder()
            .codMunicipio(codMunicipio)
            .codZonaTerritorial(codZona)
            .detalle(detalle)
            .barrio(barrio)
            .build();

        Direccion saved = direccionRepository.save(direccion);
        return saved.getId();
    }

    /**
     * Crea un registro de paciente en core.pacientes
     */
    private UUID createPatient(RegisterPatientRequest request, UUID contactId, UUID directionId) {
        log.debug("Creando paciente: {} {}", request.getNombres(), request.getApellidos());

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
            .estrato(request.getEstrato() == null ? null : request.getEstrato().shortValue())
            .idPaisOrigen(request.getIdPaisOrigen())
            .idDireccion(directionId)
            .idContacto(contactId)
            .build();

        Paciente saved = pacienteRepository.save(paciente);
        return saved.getId();
    }

    /**
     * Crea un perfil en core.profiles
     * Este perfil será vinculado a auth.users después que Supabase Auth lo cree
     */
    private UUID createProfile(String email, String nombre, String apellido) {
        log.debug("Creando perfil para email: {}", email);
        UUID profileId = UUID.randomUUID();

        // Intentamos asignar el role 'Paciente' desde el inicio para respetar el modelo
        UUID roleId = roleRepository.findByNombre("Paciente")
            .map(Role::getId)
            .orElse(null);

        Profile profile = Profile.builder()
            .id(profileId)
            .email(email)
            .name(nombre + " " + apellido)
            .passwordChangeRequired(true)
            .createdAt(LocalDateTime.now())
            .roleId(roleId)
            .build();

        Profile saved = profileRepository.save(profile);
        return saved.getId();
    }

    /**
     * Asigna el rol de "Paciente" al perfil creado
     */
    private void assignPatientRole(UUID profileId) {
        log.debug("Asignando rol de Paciente al perfil: {}", profileId);

        Role patientRole = roleRepository.findByNombre("Paciente")
            .orElseThrow(() -> new RuntimeException("Role 'Paciente' no encontrado"));

        Profile profile = profileRepository.findById(profileId)
            .orElseThrow(() -> new RuntimeException("Profile no encontrado"));

        profile.setRoleId(patientRole.getId());
        profileRepository.save(profile);
    }

    /**
     * Obtiene la configuración de Supabase Auth necesaria para el frontend
     */
    public Map<String, Object> getSupabaseAuthConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("supabaseUrl", supabaseUrl);
        config.put("supabaseAnonKey", supabaseAnonKey);
        config.put("jwkSetUri", supabaseUrl + "/auth/v1/keys");
        config.put("issuerUri", supabaseUrl + "/auth/v1");
        return config;
    }

    /**
     * Refresca un JWT token usando el refresh token
     * Nota: Esto es delegado a Supabase Auth
     */
    public Map<String, Object> refreshToken(String refreshToken) {
        log.debug("Refrescando token JWT");
        // TODO: Llamar a Supabase Auth API para refrescar token
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Implementar integración con Supabase Auth");
        return response;
    }

    /**
     * Obtiene el perfil del usuario autenticado actual
     */
    public Map<String, Object> getCurrentUserProfile() {
        log.debug("Obteniendo perfil del usuario actual");
        // Obtener el usuario autenticado desde SecurityContext
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        Map<String, Object> profile = new HashMap<>();

        if (auth == null || !auth.isAuthenticated()) {
            profile.put("error", "No authenticated user");
            return profile;
        }

        // Supabase coloca el 'sub' como subject (UUID) — usamos eso para buscar en core.profiles
        String subject = auth.getName();
        try {
            java.util.UUID profileId = java.util.UUID.fromString(subject);
            var opt = profileRepository.findById(profileId);
            if (opt.isPresent()) {
                var p = opt.get();
                profile.put("id", p.getId());
                profile.put("email", p.getEmail());
                profile.put("name", p.getName());
                profile.put("roleId", p.getRoleId());
                profile.put("message", "Profile loaded from core.profiles");
                return profile;
            } else {
                profile.put("message", "Profile not found in core.profiles");
                profile.put("subject", subject);
                return profile;
            }
        } catch (IllegalArgumentException ex) {
            profile.put("message", "Invalid subject in token: " + subject);
            return profile;
        }
    }
}
