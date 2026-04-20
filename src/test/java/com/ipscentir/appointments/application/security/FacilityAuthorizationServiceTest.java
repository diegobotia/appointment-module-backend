package com.ipscentir.appointments.application.security;

import com.ipscentir.appointments.domain.model.facility.Facility;
import com.ipscentir.appointments.domain.model.security.AppUser;
import com.ipscentir.appointments.infrastructure.persistence.jpa.AppUserJpaRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FacilityAuthorizationServiceTest {

    @Mock
    private AppUserJpaRepository appUserJpaRepository;

    @InjectMocks
    private FacilityAuthorizationService facilityAuthorizationService;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldAllowWhenUserHasFacilityAssigned() {
        UUID facilityId = UUID.randomUUID();
        setAuthentication("admin", "ROLE_ADMIN");

        AppUser user = AppUser.builder()
                .username("admin")
                .passwordHash("hash")
                .active(true)
                .facilities(Set.of(Facility.builder().id(facilityId).build()))
                .build();

        when(appUserJpaRepository.findByUsername("admin")).thenReturn(Optional.of(user));

        assertDoesNotThrow(() -> facilityAuthorizationService.assertCurrentUserCanAccessFacility(facilityId));
    }

    @Test
    void shouldDenyWhenUserDoesNotHaveFacilityAssigned() {
        UUID requestedFacility = UUID.randomUUID();
        setAuthentication("admin", "ROLE_ADMIN");

        AppUser user = AppUser.builder()
                .username("admin")
                .passwordHash("hash")
                .active(true)
                .facilities(Set.of(Facility.builder().id(UUID.randomUUID()).build()))
                .build();

        when(appUserJpaRepository.findByUsername("admin")).thenReturn(Optional.of(user));

        assertThrows(
                AccessDeniedException.class,
                () -> facilityAuthorizationService.assertCurrentUserCanAccessFacility(requestedFacility)
        );
    }

    @Test
    void shouldBypassForAnonymousRequests() {
        UUID facilityId = UUID.randomUUID();

        assertDoesNotThrow(() -> facilityAuthorizationService.assertCurrentUserCanAccessFacility(facilityId));
    }

    @Test
    void shouldReturnCurrentUserFacilityIds() {
        UUID firstFacility = UUID.randomUUID();
        UUID secondFacility = UUID.randomUUID();
        setAuthentication("admin", "ROLE_ADMIN");

        AppUser user = AppUser.builder()
                .username("admin")
                .passwordHash("hash")
                .active(true)
                .facilities(Set.of(
                        Facility.builder().id(firstFacility).build(),
                        Facility.builder().id(secondFacility).build()
                ))
                .build();

        when(appUserJpaRepository.findByUsername("admin")).thenReturn(Optional.of(user));

        Set<UUID> facilityIds = facilityAuthorizationService.getCurrentUserFacilityIds(SecurityContextHolder.getContext().getAuthentication());

        assertEquals(Set.of(firstFacility, secondFacility), facilityIds);
    }

    private void setAuthentication(String username, String role) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        username,
                        "password",
                        List.of(new SimpleGrantedAuthority(role))
                )
        );
    }
}
