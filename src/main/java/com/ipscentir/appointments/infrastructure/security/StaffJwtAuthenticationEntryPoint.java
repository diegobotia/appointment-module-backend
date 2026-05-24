package com.ipscentir.appointments.infrastructure.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.web.AuthenticationEntryPoint;

import java.io.IOException;

/**
 * Registra en logs la causa del 401 antes de responder al cliente.
 */
@Slf4j
public class StaffJwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        boolean bearerPresent = authorization != null && authorization.startsWith("Bearer ");
        String bearerHint = bearerPresent
                ? "Bearer presente (longitud=" + authorization.length() + ")"
                : "sin header Authorization";

        String stage = classifyFailure(authException);
        log.warn(
                "[AUTH-401] stage={} method={} uri={} {} exception={} message={}",
                stage,
                request.getMethod(),
                request.getRequestURI(),
                bearerHint,
                authException.getClass().getSimpleName(),
                authException.getMessage()
        );

        if (log.isDebugEnabled() && authException.getCause() != null) {
            log.debug("[AUTH-401] rootCause", authException.getCause());
        }

        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, authException.getMessage());
    }

    private static String classifyFailure(AuthenticationException exception) {
        if (exception instanceof InvalidBearerTokenException) {
            return "JWT_INVALIDO";
        }
        if (exception instanceof OAuth2AuthenticationException oauth2) {
            String errorCode = oauth2.getError().getErrorCode();
            return "OAUTH2_" + errorCode.toUpperCase();
        }
        String message = exception.getMessage() != null ? exception.getMessage() : "";
        if (message.contains("Perfil no encontrado")) {
            return "PERFIL_NO_ENCONTRADO";
        }
        if (message.contains("Perfil inactivo")) {
            return "PERFIL_INACTIVO";
        }
        if (message.contains("Rol no autorizado")) {
            return "ROL_NO_AUTORIZADO";
        }
        if (message.contains("Full authentication is required") || message.contains("Not Authenticated")) {
            return "SIN_AUTENTICACION";
        }
        return "AUTENTICACION_FALLIDA";
    }
}
