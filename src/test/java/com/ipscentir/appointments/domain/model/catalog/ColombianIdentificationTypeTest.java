package com.ipscentir.appointments.domain.model.catalog;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ColombianIdentificationTypeTest {

    @ParameterizedTest
    @CsvSource({
            "Cédula de ciudadanía, 13",
            "cedula de ciudadania, 13",
            "13, 13",
            "CC, 13",
            "Tarjeta de identidad, 12",
            "TI, 12",
            "Pasaporte, 41",
            "PEP (Permiso Especial de Permanencia), 47"
    })
    void shouldResolveToDianCodigo(String input, String expected) {
        assertEquals(expected, ColombianIdentificationType.resolveCodigo(input));
    }

    @Test
    void shouldRejectUnknownType() {
        assertThrows(IllegalArgumentException.class, () -> ColombianIdentificationType.resolveCodigo("DNI español"));
    }

    @Test
    void lookupCandidatesIncludeLegacyAlias() {
        var type = ColombianIdentificationType.CEDULA_CIUDADANIA;
        assertTrue(type.lookupCandidates().containsAll(java.util.List.of("13", "CC")));
    }
}
