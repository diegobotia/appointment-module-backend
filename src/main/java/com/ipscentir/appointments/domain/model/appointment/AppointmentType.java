package com.ipscentir.appointments.domain.model.appointment;

public enum AppointmentType {
    PRESENCIAL,
    JUNTA_MEDICA,
    TERAPIA_FISICA,
    TERAPIA_OCUPACIONAL,
    /** Reunión o bloqueo interno entre personal administrativo (sin paciente). */
    STAFF
}
