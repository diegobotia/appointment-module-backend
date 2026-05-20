package com.ipscentir.appointments.domain.model.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RoleNameTest {

    @Test
    void shouldResolveSupabaseRoleNames() {
        assertThat(RoleName.fromSupabaseNombre("Administracion")).contains(RoleName.ADMINISTRACION);
        assertThat(RoleName.fromSupabaseNombre("Medico")).contains(RoleName.MEDICO);
        assertThat(RoleName.fromSupabaseNombre("Admisiones")).contains(RoleName.ADMISIONES);
        assertThat(RoleName.fromSupabaseNombre("Facturacion")).contains(RoleName.FACTURACION);
    }

    @Test
    void shouldNotResolveLegacyRoles() {
        assertThat(RoleName.fromSupabaseNombre("Superadmin")).isEmpty();
        assertThat(RoleName.fromSupabaseNombre("Paciente")).isEmpty();
    }
}
