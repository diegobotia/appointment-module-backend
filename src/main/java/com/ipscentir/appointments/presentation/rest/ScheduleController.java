package com.ipscentir.appointments.presentation.rest;

import com.ipscentir.appointments.application.dto.ScheduleDTO;
import com.ipscentir.appointments.application.service.ScheduleApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.DayOfWeek;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/schedules")
@RequiredArgsConstructor
@Tag(name = "Schedules API", description = "Endpoints for retrieving scheduled slots and doctors availability")
public class ScheduleController {

    private final ScheduleApplicationService scheduleApplicationService;

    @GetMapping("/doctors/{doctorId}/day/{dayOfWeek}")
    @Operation(summary = "Get Doctor Schedule Configuration", description = "Retrieves the active routing template for a specific doctor and facility on a given day of the week")
    public ResponseEntity<ScheduleDTO> getDoctorSchedule(
            @PathVariable String doctorId,
            @PathVariable DayOfWeek dayOfWeek,
            @RequestParam UUID facilityId
    ) {
        return ResponseEntity.ok(scheduleApplicationService.getScheduleForDoctorAndFacility(doctorId, facilityId, dayOfWeek));
    }
}
