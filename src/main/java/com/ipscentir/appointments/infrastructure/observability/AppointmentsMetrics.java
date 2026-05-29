package com.ipscentir.appointments.infrastructure.observability;

import com.ipscentir.appointments.domain.model.appointment.BookingChannel;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class AppointmentsMetrics {

    private final MeterRegistry meterRegistry;
    private final Counter securityUnauthorized;
    private final Counter securityForbidden;

    public AppointmentsMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.securityUnauthorized = Counter.builder("security.unauthorized")
                .description("Respuestas HTTP 401")
                .register(meterRegistry);
        this.securityForbidden = Counter.builder("security.forbidden")
                .description("Respuestas HTTP 403")
                .register(meterRegistry);
    }

    public void recordAppointmentCreated(BookingChannel channel) {
        String channelTag = channel != null ? channel.name() : "UNKNOWN";
        meterRegistry.counter("appointments.created", "channel", channelTag).increment();
    }

    public void recordUnauthorized() {
        securityUnauthorized.increment();
    }

    public void recordForbidden() {
        securityForbidden.increment();
    }
}
