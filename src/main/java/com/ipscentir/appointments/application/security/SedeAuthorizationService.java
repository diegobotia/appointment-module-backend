package com.ipscentir.appointments.application.security;

import com.ipscentir.appointments.domain.model.security.RoleName;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
public class SedeAuthorizationService {

    public boolean canAccessSede(Integer sedeId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || !(authentication instanceof JwtAuthenticationToken)) {
            return true;
        }
        return hasRole(authentication, RoleName.ADMINISTRACION)
                || hasRole(authentication, RoleName.ADMISIONES)
                || hasRole(authentication, RoleName.ASESOR)
                || hasRole(authentication, RoleName.MEDICO)
                || hasRole(authentication, RoleName.FACTURACION);
    }

    public void assertCurrentUserCanAccessSede(Integer sedeId) {
        if (!canAccessSede(sedeId)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "No tiene acceso a la sede: " + sedeId
            );
        }
    }

    private boolean hasRole(Authentication authentication, RoleName roleName) {
        String authority = roleName.getAuthority();
        return authentication.getAuthorities().stream()
                .anyMatch(granted -> granted.getAuthority().equals(authority));
    }
}
