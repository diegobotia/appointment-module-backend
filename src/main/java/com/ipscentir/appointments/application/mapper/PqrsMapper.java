package com.ipscentir.appointments.application.mapper;

import com.ipscentir.appointments.application.dto.PqrsDTO;
import com.ipscentir.appointments.application.dto.form.CreatePqrsRequest;
import com.ipscentir.appointments.domain.model.pqrs.Pqrs;
import com.ipscentir.appointments.domain.model.pqrs.PqrsType;
import org.springframework.stereotype.Component;

@Component
public class PqrsMapper {

    public Pqrs toDomain(CreatePqrsRequest request, String radicado) {
        return Pqrs.create(
                request.cedula(),
                PqrsType.valueOf(request.tipo()),
                request.descripcion(),
                request.correo(),
                request.nombres(),
                request.telefono(),
                radicado
        );
    }

    public PqrsDTO toDto(Pqrs pqrs) {
        return new PqrsDTO(
                pqrs.getId(),
                pqrs.getCedula(),
                pqrs.getTipo(),
                pqrs.getDescripcion(),
                pqrs.getCorreo(),
                pqrs.getNombres(),
                pqrs.getTelefono(),
                pqrs.getRadicado(),
                pqrs.getStatus(),
                pqrs.getMetadata(),
                pqrs.getCreatedAt(),
                pqrs.getUpdatedAt()
        );
    }
}
