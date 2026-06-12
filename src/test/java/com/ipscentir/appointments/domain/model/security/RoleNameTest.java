package com.ipscentir.appointments.domain.model.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RoleNameTest {

    @Test
    void shouldResolveSupabaseRoleNames() {
        assertThat(RoleName.fromNombre("Administracion")).contains(RoleName.ADMINISTRACION);
        assertThat(RoleName.fromNombre("Medico")).contains(RoleName.MEDICO);
        assertThat(RoleName.fromNombre("Admisiones")).contains(RoleName.ADMISIONES);
        assertThat(RoleName.fromNombre("Asesor")).contains(RoleName.ASESOR);
        assertThat(RoleName.fromNombre("Facturacion")).contains(RoleName.FACTURACION);
    }

    @Test
    void shouldNotResolveLegacyRoles() {
        assertThat(RoleName.fromNombre("Superadmin")).isEmpty();
        assertThat(RoleName.fromNombre("Paciente")).isEmpty();
    }
}
