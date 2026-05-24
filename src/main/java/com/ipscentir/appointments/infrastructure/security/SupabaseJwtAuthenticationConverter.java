package com.ipscentir.appointments.infrastructure.security;

import com.ipscentir.appointments.application.security.MedicoProfileResolver;
import com.ipscentir.appointments.domain.model.security.RoleName;
import com.ipscentir.appointments.infrastructure.persistence.jpa.ProfileRepository;
import com.ipscentir.appointments.infrastructure.persistence.jpa.RoleRepository;
import com.ipscentir.appointments.infrastructure.persistence.jpa.entity.Profile;
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
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class SupabaseJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

  /** Claims de Supabase Auth; no son roles de negocio en {@code core.roles}. */
    private static final Set<String> SUPABASE_AUTH_ONLY_ROLES = Set.of("authenticated", "anon");

    private final ProfileRepository profileRepository;
    private final RoleRepository roleRepository;
    private final MedicoProfileResolver medicoProfileResolver;

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        try {
            StaffPrincipal principal = resolvePrincipal(jwt);
            Collection<GrantedAuthority> authorities = principal.roleName()
                    .map(role -> List.<GrantedAuthority>of(new SimpleGrantedAuthority(role.getAuthority())))
                    .orElse(List.of());

            JwtAuthenticationToken authentication = new JwtAuthenticationToken(
                    jwt, authorities, principal.profileId().toString());
            authentication.setDetails(principal);
            log.debug(
                    "[AUTH-OK] sub={} role={} authorities={}",
                    principal.profileId(),
                    principal.roleName().map(RoleName::name).orElse("?"),
                    authorities
            );
            return authentication;
        } catch (RuntimeException ex) {
            log.warn(
                    "[AUTH-CONVERTER] sub={} email={} jwtRole={} error={}",
                    jwt.getSubject(),
                    jwt.getClaimAsString("email"),
                    jwt.getClaimAsString("role"),
                    ex.getMessage()
            );
            throw ex;
        }
    }

    private StaffPrincipal resolvePrincipal(Jwt jwt) {
        UUID profileId = UUID.fromString(jwt.getSubject());

        Profile profile = profileRepository.findById(profileId)
                .orElseThrow(() -> new IllegalStateException("Perfil no encontrado en core.profiles: " + profileId));

        if (Boolean.FALSE.equals(profile.getEstaActivo())) {
            throw new IllegalStateException("Perfil inactivo: " + profileId);
        }

        RoleName roleName = resolveRoleName(jwt, profile)
                .orElseThrow(() -> new IllegalStateException("Rol no autorizado para el módulo de citas: " + profileId));

        log.debug("JWT autenticado profileId={} role={}", profileId, roleName);

        Optional<String> medicoId = roleName == RoleName.MEDICO
                ? Optional.of(medicoProfileResolver.requireMedicoId(profile))
                : Optional.empty();

        return new StaffPrincipal(profileId, Optional.of(roleName), true, medicoId);
    }

    /**
     * Rol de negocio: primero {@code core.profiles.role_id}, luego claims custom del JWT.
     * El claim {@code role=authenticated} de Supabase no define permisos en este módulo.
     */
    private Optional<RoleName> resolveRoleName(Jwt jwt, Profile profile) {
        Optional<RoleName> fromProfile = roleRepository.findById(profile.getRoleId())
                .flatMap(role -> RoleName.fromSupabaseNombre(role.getNombre()));

        if (fromProfile.isPresent()) {
            return fromProfile;
        }

        return extractBusinessRoleNombre(jwt).flatMap(RoleName::fromSupabaseNombre);
    }

    private Optional<String> extractBusinessRoleNombre(Jwt jwt) {
        String direct = jwt.getClaimAsString("role_nombre");
        if (isBusinessRoleNombre(direct)) {
            return Optional.of(direct);
        }

        String role = jwt.getClaimAsString("role");
        if (isBusinessRoleNombre(role)) {
            return Optional.of(role);
        }

        return extractFromMetadata(jwt.getClaim("app_metadata"))
                .or(() -> extractFromMetadata(jwt.getClaim("user_metadata")));
    }

    private static boolean isBusinessRoleNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            return false;
        }
        return !SUPABASE_AUTH_ONLY_ROLES.contains(nombre.trim().toLowerCase(Locale.ROOT));
    }

    @SuppressWarnings("unchecked")
    private Optional<String> extractFromMetadata(Object metadata) {
        if (!(metadata instanceof Map<?, ?> map)) {
            return Optional.empty();
        }
        Object roleNombre = map.get("role_nombre");
        if (roleNombre instanceof String s && isBusinessRoleNombre(s)) {
            return Optional.of(s);
        }
        Object role = map.get("role");
        if (role instanceof String s && isBusinessRoleNombre(s)) {
            return Optional.of(s);
        }
        return Optional.empty();
    }

}
