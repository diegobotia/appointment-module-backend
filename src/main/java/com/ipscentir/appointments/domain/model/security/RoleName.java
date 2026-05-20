package com.ipscentir.appointments.domain.model.security;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

/**
 * Roles internos alineados con {@code core.roles.nombre} en Supabase.
 */
public enum RoleName {
    MEDICO("Medico"),
    ADMISIONES("Admisiones"),
    ADMINISTRACION("Administracion"),
    FACTURACION("Facturacion");

    private final String supabaseNombre;

    RoleName(String supabaseNombre) {
        this.supabaseNombre = supabaseNombre;
    }

    public String getSupabaseNombre() {
        return supabaseNombre;
    }

    public String getAuthority() {
        return "ROLE_" + name();
    }

    public static Optional<RoleName> fromSupabaseNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            return Optional.empty();
        }
        String normalized = nombre.trim();
        return Arrays.stream(values())
                .filter(role -> role.supabaseNombre.equalsIgnoreCase(normalized)
                        || role.name().equalsIgnoreCase(normalized))
                .findFirst();
    }

    public static Optional<RoleName> fromAuthority(String authority) {
        if (authority == null || authority.isBlank()) {
            return Optional.empty();
        }
        String value = authority.startsWith("ROLE_")
                ? authority.substring("ROLE_".length())
                : authority;
        return Arrays.stream(values())
                .filter(role -> role.name().equalsIgnoreCase(value))
                .findFirst();
    }
}
