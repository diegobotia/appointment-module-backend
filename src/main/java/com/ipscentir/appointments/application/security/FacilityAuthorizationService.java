package com.ipscentir.appointments.application.security;

import com.ipscentir.appointments.domain.model.security.RoleName;
import com.ipscentir.appointments.infrastructure.security.StaffPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class FacilityAuthorizationService {

    public boolean canAccessFacility(UUID facilityId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || !(authentication instanceof JwtAuthenticationToken)) {
            // Flujos n8n y formularios públicos validan acceso por API key / lógica propia.
            return true;
        }

        if (hasRole(authentication, RoleName.ADMINISTRACION)) {
            return true;
        }

        StaffPrincipal principal = extractPrincipal(authentication);
        if (principal == null) {
            return false;
        }

        if (principal.facilityIds().isEmpty()) {
            return false;
        }

        return principal.facilityIds().contains(facilityId);
    }

    public void assertCurrentUserCanAccessFacility(UUID facilityId) {
        if (!canAccessFacility(facilityId)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "No tiene acceso a la sede: " + facilityId
            );
        }
    }

    private boolean hasRole(Authentication authentication, RoleName roleName) {
        String authority = roleName.getAuthority();
        return authentication.getAuthorities().stream()
                .anyMatch(granted -> granted.getAuthority().equals(authority));
    }

    private StaffPrincipal extractPrincipal(Authentication authentication) {
        if (authentication instanceof JwtAuthenticationToken jwtAuth
                && jwtAuth.getDetails() instanceof StaffPrincipal principal) {
            return principal;
        }
        return null;
    }
}
