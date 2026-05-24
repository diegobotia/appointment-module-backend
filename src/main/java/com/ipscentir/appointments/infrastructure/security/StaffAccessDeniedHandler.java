package com.ipscentir.appointments.infrastructure.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.io.IOException;
import java.util.stream.Collectors;

/**
 * Registra en logs la causa del 403 (JWT válido pero sin permiso en la ruta).
 */
@Slf4j
public class StaffAccessDeniedHandler implements AccessDeniedHandler {

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException
    ) throws IOException {
        Authentication authentication = (Authentication) request.getUserPrincipal();
        String principal = authentication != null ? authentication.getName() : "anonymous";
        String authorities = authentication != null
                ? authentication.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .collect(Collectors.joining(", "))
                : "none";

        log.warn(
                "[AUTH-403] method={} uri={} principal={} authorities=[{}] message={}",
                request.getMethod(),
                request.getRequestURI(),
                principal,
                authorities,
                accessDeniedException.getMessage()
        );

        response.sendError(HttpServletResponse.SC_FORBIDDEN, accessDeniedException.getMessage());
    }
}
