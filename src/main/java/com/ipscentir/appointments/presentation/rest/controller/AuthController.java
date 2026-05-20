package com.ipscentir.appointments.presentation.rest.controller;

import com.ipscentir.appointments.application.service.AuthApplicationService;
import com.ipscentir.appointments.application.service.dto.RegisterPatientRequest;
import com.ipscentir.appointments.application.service.dto.RegisterPatientResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * AuthController - Endpoints de autenticación y registro
 * 
 * Expone funcionalidad de:
 * - Registro de pacientes (vía n8n o frontend)
 * - Login (delegado a Supabase Auth)
 * - Refresh de tokens JWT
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Authentication", description = "Endpoints de autenticación y registro")
public class AuthController {

    private final AuthApplicationService authApplicationService;

    /**
     * POST /api/v1/auth/register
     * 
     * Registra un nuevo paciente en el sistema.
     * 
     * Esta operación:
     * 1. Valida los datos de entrada
     * 2. Crea un perfil en core.profiles (con auth.users si es necesario)
     * 3. Crea un registro en core.pacientes con los datos clínicos
     * 4. Asigna el rol de "Paciente"
     * 5. Devuelve el ID creado
     * 
     * @param request datos del registro del paciente
     * @return respuesta con ID del paciente creado
     */
    @PostMapping("/register")
    @Operation(
        summary = "Registrar nuevo paciente",
        description = "Crea un nuevo paciente en el sistema. Acepta llamadas desde n8n o frontend."
    )
    public ResponseEntity<RegisterPatientResponse> registerPatient(
            @Valid @RequestBody RegisterPatientRequest request
    ) {
        log.info("Registrando nuevo paciente: {} {}", request.getNombres(), request.getApellidos());
        
        RegisterPatientResponse response = authApplicationService.registerPatient(request);
        
        log.info("Paciente registrado exitosamente: {}", response.getPatientId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /api/v1/auth/supabase-config
     * 
     * Devuelve configuración de Supabase Auth para el frontend.
     * Esta información es pública y necesaria para que el cliente pueda
     * comunicarse con Supabase Auth.
     * 
     * @return configuración necesaria para autenticación
     */
    @GetMapping("/supabase-config")
    @Operation(
        summary = "Obtener configuración de Supabase Auth",
        description = "Devuelve URLs y configuración necesaria para que el cliente se conecte a Supabase Auth"
    )
    public ResponseEntity<?> getSupabaseConfig() {
        // Esta información es pública
        return ResponseEntity.ok(authApplicationService.getSupabaseAuthConfig());
    }

    /**
     * POST /api/v1/auth/refresh
     * 
     * Refresca un JWT token usando el refresh token.
     * El cliente debe enviar el refresh_token obtenido en el login.
     * 
     * @param refreshToken token de refresco
     * @return nuevo JWT
     */
    @PostMapping("/refresh")
    @Operation(
        summary = "Refrescar JWT token",
        description = "Obtiene un nuevo JWT usando el refresh token"
    )
    public ResponseEntity<?> refreshToken(
            @RequestParam String refreshToken
    ) {
        log.debug("Refrescando token JWT");
        return ResponseEntity.ok(authApplicationService.refreshToken(refreshToken));
    }

    /**
     * GET /api/v1/auth/me
     * 
     * Obtiene el perfil del usuario actualmente autenticado.
     * Requiere un JWT válido en el header Authorization.
     * 
     * @return perfil del usuario autenticado
     */
    @GetMapping("/me")
    @Operation(
        summary = "Obtener perfil del usuario actual",
        description = "Devuelve la información del usuario autenticado"
    )
    public ResponseEntity<?> getCurrentUser() {
        log.debug("Obteniendo perfil del usuario actual");
        return ResponseEntity.ok(authApplicationService.getCurrentUserProfile());
    }
}
