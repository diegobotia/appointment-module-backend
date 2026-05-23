package com.ipscentir.appointments.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ipscentir.appointments.application.dto.admin.N8nJournalPageResponse;
import com.ipscentir.appointments.domain.model.appointment.BookingChannel;
import com.ipscentir.appointments.domain.model.integration.DomainEventRecord;
import com.ipscentir.appointments.domain.repository.AppointmentRepository;
import com.ipscentir.appointments.domain.repository.DomainEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminN8nJournalServiceTest {

    @Mock
    private DomainEventRepository domainEventRepository;

    @Mock
    private AppointmentRepository appointmentRepository;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private AdminN8nJournalService adminN8nJournalService;

    @Test
    void searchReturnsPagedJournalWithChannelCounts() {
        UUID eventId = UUID.randomUUID();
        DomainEventRecord record = DomainEventRecord.builder()
                .id(eventId)
                .eventType("AppointmentCreatedEvent")
                .aggregateId(UUID.randomUUID())
                .eventData("{\"bookingChannel\":\"N8N\",\"n8nConversationId\":\"conv-1\"}")
                .published(true)
                .publishedAt(LocalDateTime.now())
                .occurredOn(LocalDateTime.now())
                .build();

        when(domainEventRepository.countSearch("AppointmentCreatedEvent", true)).thenReturn(1L);
        when(domainEventRepository.search("AppointmentCreatedEvent", true, 0, 20))
                .thenReturn(List.of(record));
        when(appointmentRepository.countByBookingChannel(BookingChannel.N8N)).thenReturn(3L);
        when(appointmentRepository.countByBookingChannel(BookingChannel.STAFF)).thenReturn(7L);

        N8nJournalPageResponse response = adminN8nJournalService.search("AppointmentCreatedEvent", true, 0, 20);

        assertEquals(1, response.content().size());
        assertEquals(BookingChannel.N8N, response.content().getFirst().bookingChannel());
        assertEquals("conv-1", response.content().getFirst().n8nConversationId());
        assertEquals(3L, response.appointmentsCreatedByN8n());
        assertEquals(7L, response.appointmentsCreatedByStaff());
    }

    @Test
    void bookingChannelSummaryAggregatesTotals() {
        when(appointmentRepository.countByBookingChannel(BookingChannel.N8N)).thenReturn(2L);
        when(appointmentRepository.countByBookingChannel(BookingChannel.STAFF)).thenReturn(5L);

        var summary = adminN8nJournalService.bookingChannelSummary();

        assertNotNull(summary);
        assertEquals(2L, summary.n8nCount());
        assertEquals(5L, summary.staffCount());
        assertEquals(7L, summary.total());
    }

    @Test
    void searchNormalizesInvalidPagination() {
        when(domainEventRepository.countSearch(any(), eq(null))).thenReturn(0L);
        when(domainEventRepository.search(any(), eq(null), eq(0), eq(20))).thenReturn(List.of());
        when(appointmentRepository.countByBookingChannel(any())).thenReturn(0L);

        N8nJournalPageResponse response = adminN8nJournalService.search(null, null, -1, 0);

        assertEquals(0, response.page());
        assertEquals(20, response.size());
    }
}
