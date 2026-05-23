package com.ipscentir.appointments.domain.model.appointment;

/**
 * Indica quién originó la acción de agendamiento.
 */
public enum BookingChannel {
    /** Chat / flujo conversacional n8n */
    N8N,
    /** Mostrador o panel interno (Admisiones / Administración) */
    STAFF
}
