package com.ipscentir.appointments.infrastructure.security;

import com.ipscentir.appointments.domain.model.security.RoleName;
import com.ipscentir.appointments.infrastructure.persistence.jpa.ProfileRepository;
import com.ipscentir.appointments.infrastructure.persistence.jpa.RoleRepository;
import com.ipscentir.appointments.infrastructure.persistence.jpa.entity.Profile;
import com.ipscentir.appointments.infrastructure.persistence.jpa.entity.Role;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class SupabaseJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final ProfileRepository profileRepository;
    private final RoleRepository roleRepository;

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        StaffPrincipal principal = resolvePrincipal(jwt);
        Collection<GrantedAuthority> authorities = principal.roleName()
                .map(role -> List.<GrantedAuthority>of(new SimpleGrantedAuthority(role.getAuthority())))
                .orElse(List.of());

        JwtAuthenticationToken authentication = new JwtAuthenticationToken(jwt, authorities, principal.profileId().toString());
        authentication.setDetails(principal);
        return authentication;
    }

    private StaffPrincipal resolvePrincipal(Jwt jwt) {
        UUID profileId = UUID.fromString(jwt.getSubject());

        Profile profile = profileRepository.findById(profileId)
                .orElseThrow(() -> new IllegalStateException("Perfil no encontrado en core.profiles: " + profileId));

        if (Boolean.FALSE.equals(profile.getEstaActivo())) {
            throw new IllegalStateException("Perfil inactivo: " + profileId);
        }

        RoleName roleName = extractRoleNombre(jwt)
                .flatMap(RoleName::fromSupabaseNombre)
                .or(() -> roleRepository.findById(profile.getRoleId())
                        .flatMap(role -> RoleName.fromSupabaseNombre(role.getNombre())))
                .orElseThrow(() -> new IllegalStateException("Rol no autorizado para el módulo de citas: " + profileId));

        return new StaffPrincipal(profileId, Optional.of(roleName), extractFacilityIds(jwt), true);
    }

    private Optional<String> extractRoleNombre(Jwt jwt) {
        String direct = jwt.getClaimAsString("role_nombre");
        if (direct != null && !direct.isBlank()) {
            return Optional.of(direct);
        }

        String role = jwt.getClaimAsString("role");
        if (role != null && !role.isBlank()) {
            return Optional.of(role);
        }

        return extractFromMetadata(jwt.getClaim("app_metadata"))
                .or(() -> extractFromMetadata(jwt.getClaim("user_metadata")));
    }

    @SuppressWarnings("unchecked")
    private Optional<String> extractFromMetadata(Object metadata) {
        if (!(metadata instanceof Map<?, ?> map)) {
            return Optional.empty();
        }
        Object roleNombre = map.get("role_nombre");
        if (roleNombre instanceof String s && !s.isBlank()) {
            return Optional.of(s);
        }
        Object role = map.get("role");
        if (role instanceof String s && !s.isBlank()) {
            return Optional.of(s);
        }
        return Optional.empty();
    }

    @SuppressWarnings("unchecked")
    private List<UUID> extractFacilityIds(Jwt jwt) {
        Object claim = jwt.getClaim("facility_ids");
        if (claim instanceof List<?> list) {
            return list.stream()
                    .map(Object::toString)
                    .map(UUID::fromString)
                    .toList();
        }
        return List.of();
    }
}
