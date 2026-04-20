package com.ipscentir.appointments.presentation.rest;

import com.ipscentir.appointments.application.dto.security.AuthUserDTO;
import com.ipscentir.appointments.application.dto.security.LoginRequest;
import com.ipscentir.appointments.application.security.FacilityAuthorizationService;
import com.ipscentir.appointments.application.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Auth API", description = "Authentication endpoints for internal users")
public class AuthController {

    private final AuthService authService;
    private final FacilityAuthorizationService facilityAuthorizationService;

    @PostMapping("/login")
    @Operation(summary = "Login", description = "Creates authenticated session for internal frontend")
    public ResponseEntity<AuthUserDTO> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpServletRequest
    ) {
        return ResponseEntity.ok(authService.login(request, httpServletRequest));
    }

    @GetMapping("/me")
    @Operation(summary = "Authenticated user", description = "Returns current authenticated user and authorities")
    public ResponseEntity<AuthUserDTO> me(Authentication authentication) {
        Set<String> roles = authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        return ResponseEntity.ok(
            new AuthUserDTO(
                authentication.getName(),
                roles,
                "SESSION",
                facilityAuthorizationService.getCurrentUserFacilityIds(authentication)
            )
        );
    }
}
