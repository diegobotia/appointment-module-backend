package com.ipscentir.appointments.infrastructure.security;

import com.ipscentir.appointments.domain.model.security.RoleName;

import java.util.Optional;
import java.util.UUID;

public record StaffPrincipal(
        UUID profileId,
        Optional<RoleName> roleName,
        boolean active
) {
}
