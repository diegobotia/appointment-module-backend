package com.ipscentir.appointments.presentation.rest.controller;

import com.ipscentir.appointments.application.service.AuthApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Autenticación del personal interno (roles en core.profiles + Supabase Auth).
 * Los pacientes no se autentican: interactúan vía n8n y formulario público.
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Authentication", description = "Autenticación del personal interno")
public class AuthController {

    private final AuthApplicationService authApplicationService;

    @GetMapping("/supabase-config")
    @Operation(summary = "Obtener configuración de Supabase Auth para el panel interno")
    public ResponseEntity<?> getSupabaseConfig() {
        return ResponseEntity.ok(authApplicationService.getSupabaseAuthConfig());
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refrescar JWT token")
    public ResponseEntity<?> refreshToken(@RequestParam String refreshToken) {
        log.debug("Refrescando token JWT");
        return ResponseEntity.ok(authApplicationService.refreshToken(refreshToken));
    }

    @GetMapping("/me")
    @Operation(summary = "Obtener perfil del usuario autenticado")
    public ResponseEntity<?> getCurrentUser() {
        log.debug("Obteniendo perfil del usuario actual");
        return ResponseEntity.ok(authApplicationService.getCurrentUserProfile());
    }
}
