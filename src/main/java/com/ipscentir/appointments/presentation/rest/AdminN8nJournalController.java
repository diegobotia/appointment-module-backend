package com.ipscentir.appointments.presentation.rest;

import com.ipscentir.appointments.application.dto.admin.BookingChannelSummaryDTO;
import com.ipscentir.appointments.application.dto.admin.N8nJournalPageResponse;
import com.ipscentir.appointments.application.service.AdminN8nJournalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/n8n/journal")
@PreAuthorize("hasRole('ADMINISTRACION')")
@RequiredArgsConstructor
@Tag(name = "Admin n8n Journal", description = "Auditoría de citas creadas/reprogramadas vía chat vs mostrador")
public class AdminN8nJournalController {

    private final AdminN8nJournalService adminN8nJournalService;

    @GetMapping
    @Operation(summary = "Journal de eventos de integración n8n (creación, reprogramación, cancelación)")
    public ResponseEntity<N8nJournalPageResponse> search(
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) Boolean published,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(adminN8nJournalService.search(eventType, published, page, size));
    }

    @GetMapping("/booking-channel-summary")
    @Operation(summary = "Resumen: citas agendadas por n8n vs personal IPS")
    public ResponseEntity<BookingChannelSummaryDTO> bookingChannelSummary() {
        return ResponseEntity.ok(adminN8nJournalService.bookingChannelSummary());
    }
}
