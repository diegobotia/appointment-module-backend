package com.ipscentir.appointments.domain.service;

import java.time.LocalDate;

/**
 * Simple enum of fixed-date public holidays in Colombia.
 * Movable feasts (Easter-related) are NOT included here.
 */
public enum ColombiaPublicHoliday {
    NEW_YEAR(1, 1, "Año Nuevo"),
    KINGS_DAY(1, 11, "Reyes Magos"),
    ST_JOSEPH(3, 19, "San José"),
    LABOR_DAY(5, 1, "Día del Trabajo"),
    ST_PETER_AND_PAUL(7, 1, "San Pedro y San Pablo"),
    INDEPENDENCE_DAY(7, 20, "Grito de Independencia"),
    BOYACA_BATTLE(8, 7, "Batalla de Boyacá"),
    ASSUMPTION(8, 15, "Asunción de María"),
    ALL_SAINTS(11, 1, "Todos los Santos"),
    CARTAGENA_INDEPENDENCE(11, 11, "Independencia de Cartagena"),
    IMMACULATE_CONCEPTION(12, 8, "Inmaculada Concepción"),
    CHRISTMAS(12, 25, "Navidad");

    private final int month;
    private final int day;
    private final String displayName;

    ColombiaPublicHoliday(int month, int day, String displayName) {
        this.month = month;
        this.day = day;
        this.displayName = displayName;
    }

    public static boolean isHoliday(LocalDate date) {
        return java.util.Arrays.stream(values())
                .anyMatch(h -> h.month == date.getMonthValue() && h.day == date.getDayOfMonth());
    }

    public static String getHolidayName(LocalDate date) {
        return java.util.Arrays.stream(values())
                .filter(h -> h.month == date.getMonthValue() && h.day == date.getDayOfMonth())
                .map(h -> h.displayName)
                .findFirst()
                .orElse(null);
    }
}
