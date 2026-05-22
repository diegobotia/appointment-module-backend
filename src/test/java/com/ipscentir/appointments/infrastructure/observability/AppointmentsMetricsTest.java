package com.ipscentir.appointments.infrastructure.observability;

import com.ipscentir.appointments.domain.model.appointment.BookingChannel;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AppointmentsMetricsTest {

    private SimpleMeterRegistry meterRegistry;
    private AppointmentsMetrics metrics;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        metrics = new AppointmentsMetrics(meterRegistry);
    }

    @Test
    void recordsAppointmentCreatedWithChannelTag() {
        metrics.recordAppointmentCreated(BookingChannel.N8N);
        metrics.recordAppointmentCreated(BookingChannel.STAFF);

        assertEquals(1.0, meterRegistry.get("appointments.created").tag("channel", "N8N").counter().count());
        assertEquals(1.0, meterRegistry.get("appointments.created").tag("channel", "STAFF").counter().count());
    }

    @Test
    void recordsSecurityAndNotificationCounters() {
        metrics.recordUnauthorized();
        metrics.recordForbidden();
        metrics.recordNotificationFailed();

        assertEquals(1.0, meterRegistry.get("security.unauthorized").counter().count());
        assertEquals(1.0, meterRegistry.get("security.forbidden").counter().count());
        assertEquals(1.0, meterRegistry.get("notifications.failed").counter().count());
    }
}
