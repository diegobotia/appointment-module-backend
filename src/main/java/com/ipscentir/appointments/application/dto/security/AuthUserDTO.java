package com.ipscentir.appointments.application.dto.security;

import java.util.Set;

public record AuthUserDTO(
        String username,
        Set<String> roles,
        String authType
) {
}
