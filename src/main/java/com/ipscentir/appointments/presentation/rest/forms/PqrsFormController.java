package com.ipscentir.appointments.presentation.rest.forms;

import com.ipscentir.appointments.application.dto.form.CreatePqrsRequest;
import com.ipscentir.appointments.application.dto.form.PqrsResponse;
import com.ipscentir.appointments.application.service.PqrsApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/forms/pqrs")
@RequiredArgsConstructor
@Tag(name = "PQRS Forms API", description = "Public endpoint for submitting PQRS (Petitions, Complaints, Claims, Suggestions)")
public class PqrsFormController {

    private final PqrsApplicationService pqrsApplicationService;

    @PostMapping
    @Operation(summary = "Submit a new PQRS form")
    public ResponseEntity<PqrsResponse> submitPqrs(@Valid @RequestBody CreatePqrsRequest request) {
        PqrsResponse response = pqrsApplicationService.createPqrs(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
