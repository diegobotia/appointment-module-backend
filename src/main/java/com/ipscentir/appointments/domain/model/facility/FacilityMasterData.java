package com.ipscentir.appointments.domain.model.facility;

import java.time.LocalTime;
import java.util.List;
import java.util.Map;

/**
 * Datos operativos por sede (horario e inventario físico) para el módulo de citas.
 * Los maestros de sede viven en {@code core.sede}.
 */
public final class FacilityMasterData {

    public static final int SEDE_ID_CONQUISTADORES = 1;
    public static final int SEDE_ID_BELEN = 2;

    public static final String CODE_CONQUISTADORES = "SEDE_CONQUISTADORES";
    public static final String CODE_BELEN = "SEDE_BELEN";
    public static final String LEGACY_CODE_PRINCIPAL = "SEDE_PRINCIPAL";
    public static final String LEGACY_CODE_NORTE = "SEDE_NORTE";

    private FacilityMasterData() {
    }

    public record SedeOperationalSeed(int sedeId, List<String> legacyAliases) {
    }

    public record OperatingHourSeed(int dayOfWeek, LocalTime openTime, LocalTime closeTime, boolean closed) {
    }

    public record ResourceSeed(
            FacilityResourceType resourceType,
            String code,
            String displayName,
            int capacityUnits
    ) {
    }

    public static List<SedeOperationalSeed> defaultSedes() {
        return List.of(
                new SedeOperationalSeed(SEDE_ID_CONQUISTADORES, List.of(LEGACY_CODE_PRINCIPAL, CODE_CONQUISTADORES)),
                new SedeOperationalSeed(SEDE_ID_BELEN, List.of(LEGACY_CODE_NORTE, CODE_BELEN))
        );
    }

    public static List<OperatingHourSeed> defaultOperatingHours() {
        return List.of(
                new OperatingHourSeed(1, LocalTime.of(7, 0), LocalTime.of(18, 0), false),
                new OperatingHourSeed(2, LocalTime.of(7, 0), LocalTime.of(18, 0), false),
                new OperatingHourSeed(3, LocalTime.of(7, 0), LocalTime.of(18, 0), false),
                new OperatingHourSeed(4, LocalTime.of(7, 0), LocalTime.of(18, 0), false),
                new OperatingHourSeed(5, LocalTime.of(7, 0), LocalTime.of(18, 0), false),
                new OperatingHourSeed(6, LocalTime.of(8, 0), LocalTime.of(12, 0), false),
                new OperatingHourSeed(7, null, null, true)
        );
    }

    private static final Map<Integer, List<ResourceSeed>> RESOURCES_BY_SEDE = Map.of(
            SEDE_ID_CONQUISTADORES, List.of(
                    resource(FacilityResourceType.CONSULTORIO, "CONQ-CONS-01", "Consultorio 1"),
                    resource(FacilityResourceType.CONSULTORIO, "CONQ-CONS-02", "Consultorio 2"),
                    resource(FacilityResourceType.CONSULTORIO, "CONQ-CONS-03", "Consultorio 3"),
                    resource(FacilityResourceType.CONSULTORIO, "CONQ-CONS-04", "Consultorio 4"),
                    resource(FacilityResourceType.FISIOTERAPIA, "CONQ-FISIO-01", "Sala fisioterapia 1"),
                    resource(FacilityResourceType.FISIOTERAPIA, "CONQ-FISIO-02", "Sala fisioterapia 2"),
                    resource(FacilityResourceType.TERAPIA_OCUPACIONAL, "CONQ-TO-01", "Sala terapia ocupacional")
            ),
            SEDE_ID_BELEN, List.of(
                    resource(FacilityResourceType.CONSULTORIO, "BEL-CONS-01", "Consultorio 1"),
                    resource(FacilityResourceType.FISIOTERAPIA, "BEL-FISIO-01", "Sala fisioterapia 1"),
                    resource(FacilityResourceType.TERAPIA_OCUPACIONAL, "BEL-TO-01", "Sala terapia ocupacional")
            )
    );

    public static List<ResourceSeed> resourcesFor(int sedeId) {
        return RESOURCES_BY_SEDE.getOrDefault(sedeId, List.of());
    }

    public static int expectedConsultorioCount(int sedeId) {
        return (int) resourcesFor(sedeId).stream()
                .filter(r -> r.resourceType() == FacilityResourceType.CONSULTORIO)
                .count();
    }

    private static ResourceSeed resource(FacilityResourceType type, String code, String displayName) {
        return new ResourceSeed(type, code, displayName, 1);
    }
}
