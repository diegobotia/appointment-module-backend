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
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class StaffJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

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
                    "[AUTH-OK] email={} profileId={} role={} authorities={}",
                    jwt.getSubject(), principal.profileId(),
                    principal.roleName().map(RoleName::name).orElse("?"),
                    authorities
            );
            return authentication;
        } catch (RuntimeException ex) {
            log.warn(
                    "[AUTH-CONVERTER] sub={} role={} error={}",
                    jwt.getSubject(),
                    jwt.getClaimAsString("role"),
                    ex.getMessage()
            );
            throw ex;
        }
    }

    private StaffPrincipal resolvePrincipal(Jwt jwt) {
        String email = jwt.getSubject();

        Profile profile = profileRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Perfil no encontrado para email: " + email));

        if (Boolean.FALSE.equals(profile.getEstaActivo())) {
            throw new IllegalStateException("Perfil inactivo: " + profile.getId());
        }

        RoleName roleName = resolveRoleName(profile)
                .orElseThrow(() -> new IllegalStateException("Rol no autorizado para el módulo de citas: " + profile.getId()));

        log.debug("JWT autenticado profileId={} email={} role={}", profile.getId(), email, roleName);

        Optional<String> medicoId = roleName == RoleName.MEDICO
                ? Optional.of(medicoProfileResolver.requireMedicoId(profile))
                : Optional.empty();

        return new StaffPrincipal(profile.getId(), Optional.of(roleName), true, medicoId);
    }

    private Optional<RoleName> resolveRoleName(Profile profile) {
        return roleRepository.findById(profile.getRoleId())
                .flatMap(role -> RoleName.fromNombre(role.getNombre()));
    }

}
