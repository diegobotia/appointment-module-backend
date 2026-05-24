package com.ipscentir.appointments.domain.model.security;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

/**
 * Roles internos alineados con {@code core.roles.nombre} en Supabase.
 */
public enum RoleName {
    MEDICO("Medico"),
    ADMISIONES("Admisiones"),
    ASESOR("Asesor"),
    ADMINISTRACION("Administracion"),
    FACTURACION("Facturacion");

    /**
     * Mostrador y call center: mismos permisos operativos de citas.
     * {@link #ASESOR} es equivalente a {@link #ADMISIONES} en el módulo de citas.
     */
    public static final RoleName[] FRONT_DESK_ROLES = {ADMISIONES, ASESOR};

    /** Roles que operan el ciclo de vida de citas (incluye administración). */
    public static final RoleName[] APPOINTMENT_OPERATORS = {
            ADMINISTRACION, ADMISIONES, ASESOR
    };

    /** Roles con lectura operativa de citas (incluye facturación). */
    public static final RoleName[] APPOINTMENT_READERS = {
            ADMINISTRACION, ADMISIONES, ASESOR, FACTURACION
    };

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

    public boolean managesAppointments() {
        return this == ADMINISTRACION || isFrontDesk();
    }

    /** Mismos permisos operativos que Admisiones (sin panel de configuración admin). */
    public boolean isFrontDesk() {
        return this == ADMISIONES || this == ASESOR;
    }

    public static Optional<RoleName> fromSupabaseNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            return Optional.empty();
        }
        String normalized = normalizeNombre(nombre);
        return Arrays.stream(values())
                .filter(role -> normalizeNombre(role.supabaseNombre).equals(normalized)
                        || role.name().equalsIgnoreCase(normalized))
                .findFirst();
    }

    private static String normalizeNombre(String nombre) {
        String trimmed = nombre.trim();
        String decomposed = Normalizer.normalize(trimmed, Normalizer.Form.NFD);
        String withoutAccents = decomposed.replaceAll("\\p{M}+", "");
        return withoutAccents.toLowerCase(Locale.ROOT);
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
