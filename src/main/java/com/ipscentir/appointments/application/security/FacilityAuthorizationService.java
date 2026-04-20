package com.ipscentir.appointments.application.security;

import com.ipscentir.appointments.domain.model.security.AppUser;
import com.ipscentir.appointments.infrastructure.persistence.jpa.AppUserJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FacilityAuthorizationService {

    private static final String SUPERADMIN_ROLE = "ROLE_SUPERADMIN";

    private final AppUserJpaRepository appUserJpaRepository;

    public void assertCurrentUserCanAccessFacility(UUID facilityId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication instanceof AnonymousAuthenticationToken || !authentication.isAuthenticated()) {
            return;
        }

        if (hasAuthority(authentication, SUPERADMIN_ROLE)) {
            return;
        }

        AppUser user = appUserJpaRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new AccessDeniedException("Usuario autenticado no existe."));

        boolean allowed = user.getFacilities().stream()
                .anyMatch(facility -> facility.getId().equals(facilityId));

        if (!allowed) {
            throw new AccessDeniedException("No tiene permiso sobre la sede solicitada.");
        }
    }

    public Set<UUID> getCurrentUserFacilityIds(Authentication authentication) {
        if (authentication == null || authentication instanceof AnonymousAuthenticationToken || !authentication.isAuthenticated()) {
            return Set.of();
        }

        return appUserJpaRepository.findByUsername(authentication.getName())
                .map(user -> user.getFacilities().stream().map(facility -> facility.getId()).collect(Collectors.toSet()))
                .orElse(Set.of());
    }

    private boolean hasAuthority(Authentication authentication, String authority) {
        return authentication.getAuthorities().stream()
                .anyMatch(grantedAuthority -> authority.equals(grantedAuthority.getAuthority()));
    }
}
