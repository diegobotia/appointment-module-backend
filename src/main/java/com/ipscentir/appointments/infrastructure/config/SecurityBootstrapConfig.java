package com.ipscentir.appointments.infrastructure.config;

import com.ipscentir.appointments.domain.model.security.AppUser;
import com.ipscentir.appointments.domain.model.security.Role;
import com.ipscentir.appointments.domain.model.security.RoleName;
import com.ipscentir.appointments.infrastructure.persistence.jpa.AppUserJpaRepository;
import com.ipscentir.appointments.infrastructure.persistence.jpa.RoleJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Set;
import java.util.UUID;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class SecurityBootstrapConfig {

        @Value("${security.bootstrap.superadmin-password:}")
        private String superadminPassword;

        @Value("${security.bootstrap.admin-password:}")
        private String adminPassword;

        @Value("${security.bootstrap.especialista-password:}")
        private String especialistaPassword;

    @Bean
    CommandLineRunner bootstrapUsersAndRoles(
            RoleJpaRepository roleJpaRepository,
            AppUserJpaRepository appUserJpaRepository,
            PasswordEncoder passwordEncoder
    ) {
        return args -> {
            Role superadminRole = roleJpaRepository.findByName(RoleName.SUPERADMIN)
                    .orElseGet(() -> roleJpaRepository.save(Role.builder().name(RoleName.SUPERADMIN).build()));
            Role adminRole = roleJpaRepository.findByName(RoleName.ADMIN)
                    .orElseGet(() -> roleJpaRepository.save(Role.builder().name(RoleName.ADMIN).build()));
            Role especialistaRole = roleJpaRepository.findByName(RoleName.ESPECIALISTA)
                    .orElseGet(() -> roleJpaRepository.save(Role.builder().name(RoleName.ESPECIALISTA).build()));

            ensureUser("superadmin", resolvePassword(superadminPassword, "superadmin"), Set.of(superadminRole), appUserJpaRepository, passwordEncoder);
            ensureUser("admin", resolvePassword(adminPassword, "admin"), Set.of(adminRole), appUserJpaRepository, passwordEncoder);
            ensureUser("especialista", resolvePassword(especialistaPassword, "especialista"), Set.of(especialistaRole), appUserJpaRepository, passwordEncoder);

            log.info("Security bootstrap completed (default users ensured)");
        };
    }

    private void ensureUser(
            String username,
            String rawPassword,
            Set<Role> roles,
            AppUserJpaRepository appUserJpaRepository,
            PasswordEncoder passwordEncoder
    ) {
        if (appUserJpaRepository.findByUsername(username).isEmpty()) {
            appUserJpaRepository.save(
                    AppUser.builder()
                            .username(username)
                            .passwordHash(passwordEncoder.encode(rawPassword))
                            .active(true)
                            .roles(roles)
                            .build()
            );
        }
    }

    private String resolvePassword(String configuredPassword, String username) {
        if (configuredPassword != null && !configuredPassword.isBlank()) {
            return configuredPassword;
        }

        String generated = UUID.randomUUID().toString();
        log.warn("No bootstrap password configured for user '{}'. Generated ephemeral password for this startup.", username);
        return generated;
    }
}
