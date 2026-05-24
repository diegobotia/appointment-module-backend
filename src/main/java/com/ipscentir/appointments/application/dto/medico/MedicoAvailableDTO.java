package com.ipscentir.appointments.application.dto.medico;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MedicoAvailableDTO {
    private String medicoId;
    private String name;
    private List<String> specialties;
}
