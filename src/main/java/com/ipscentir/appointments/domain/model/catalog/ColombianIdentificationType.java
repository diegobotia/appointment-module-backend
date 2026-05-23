package com.ipscentir.appointments.domain.model.catalog;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static java.util.Locale.ROOT;

/**
 * Catálogo DIAN de tipos de identificación (código + descripción oficial).
 * El flujo n8n envía la descripción; en BD se persiste el código.
 */
@Getter
@RequiredArgsConstructor
public enum ColombianIdentificationType {

    CERTIFICADO_NACIDO_VIVO("10", "Certificado de nacido vivo"),
    REGISTRO_CIVIL("11", "Registro civil", "RC"),
    TARJETA_IDENTIDAD("12", "Tarjeta de identidad", "TI"),
    CEDULA_CIUDADANIA("13", "Cédula de ciudadanía", "CC"),
    TARJETA_EXTRANJERIA("21", "Tarjeta de extranjería", "TE"),
    CEDULA_EXTRANJERIA("22", "Cédula de extranjería", "CE"),
    NIT("31", "NIT"),
    PASAPORTE("41", "Pasaporte", "PA"),
    DOCUMENTO_EXTRANJERO("42", "Documento de identificación extranjero"),
    PEP("47", "PEP (Permiso Especial de Permanencia)"),
    PPT("48", "PPT (Permiso Protección Temporal)"),
    NIT_OTRO_PAIS("50", "NIT de otro país"),
    NUIP("91", "NUIP");

    private final String codigo;
    private final String descripcion;
    private final Set<String> aliases;

    ColombianIdentificationType(String codigo, String descripcion, String... aliases) {
        this.codigo = codigo;
        this.descripcion = descripcion;
        this.aliases = Set.of(aliases);
    }

    public static Optional<ColombianIdentificationType> tryResolve(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        String trimmed = value.trim();
        return Arrays.stream(values())
                .filter(type -> type.matches(trimmed))
                .findFirst();
    }

    public static String resolveCodigo(String value) {
        return tryResolve(value)
                .map(ColombianIdentificationType::getCodigo)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Tipo de identificación no reconocido: " + value
                                + ". Use la descripción oficial (ej. Cédula de ciudadanía) o el código DIAN."
                ));
    }

    public static Optional<ColombianIdentificationType> fromCodigo(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            return Optional.empty();
        }
        String trimmed = codigo.trim();
        return Arrays.stream(values())
                .filter(type -> type.codigo.equals(trimmed))
                .findFirst();
    }

    /**
     * Valores a probar en consultas cuando en BD aún hay alias legacy (CC, TI, etc.).
     */
    public List<String> lookupCandidates() {
        return java.util.stream.Stream.concat(
                java.util.stream.Stream.of(codigo),
                aliases.stream()
        ).distinct().toList();
    }

    public boolean matches(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        String normalized = normalize(value);
        return normalize(codigo).equals(normalized)
                || normalize(descripcion).equals(normalized)
                || aliases.stream().anyMatch(alias -> normalize(alias).equals(normalized));
    }

    private static String normalize(String value) {
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return normalized.trim().toLowerCase(ROOT)
                .replaceAll("[^a-z0-9]+", "");
    }
}
