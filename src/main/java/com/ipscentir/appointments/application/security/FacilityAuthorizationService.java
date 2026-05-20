package com.ipscentir.appointments.application.security;

import org.springframework.stereotype.Service;
import java.util.UUID;

@Service
public class FacilityAuthorizationService {

    public boolean canAccessFacility(String username, UUID facilityId) {
        // En Supabase, si está autenticado como admin/superadmin tiene acceso.
        // TODO: Implementar lógica basada en core.profiles si es necesario.
        return true; 
    }

    public void assertCurrentUserCanAccessFacility(UUID facilityId) {
        // Stub for now.
    }
}
