package com.ipscentir.appointments.application.service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DoctorAvailableDTO {
    private String doctorId;
    private String name;
    private List<String> specialties;
}
