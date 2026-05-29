package com.ipscentir.appointments.application.security;

import com.ipscentir.appointments.domain.model.security.RoleName;
import com.ipscentir.appointments.domain.repository.ScheduleRepository;
import com.ipscentir.appointments.infrastructure.security.StaffPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class SedeAuthorizationService {

    private static final Set<RoleName> ADMIN_ROLES = Set.of(
            RoleName.ADMINISTRACION,
            RoleName.ADMISIONES,
            RoleName.ASESOR,
            RoleName.FACTURACION
    );

    private static final Set<RoleName> SEDE_ACCESS_ROLES_FALLBACK = Set.of(
            RoleName.ADMINISTRACION,
            RoleName.ADMISIONES,
            RoleName.ASESOR,
            RoleName.MEDICO,
            RoleName.FACTURACION
    );

    private static final RoleName MEDICO_ROLE = RoleName.MEDICO;

    private final StaffSecurityHelper staffSecurityHelper;
    private final ScheduleRepository scheduleRepository;

    /**
     * Valida que el usuario autenticado tenga acceso a la sede indicada.
     *
     * Reglas por rol:
     * - ADMINISTRACION, ADMISIONES, ASESOR, FACTURACION: acceso a todas las sedes.
     * - MEDICO: acceso solo a sedes donde tenga horarios asignados.
     * - n8n (API Key): acceso completo (el API key ya fue validado por N8nApiKeyFilter).
     * - No autenticado: denegado.
     */
    public boolean canAccessSede(Integer sedeId) {
        return staffSecurityHelper.currentStaffPrincipal()
                .map(principal -> principal.roleName()
                        .map(role -> {
                            if (ADMIN_ROLES.contains(role)) return true;
                            if (role == MEDICO_ROLE) return medicoHasSedeAccess(principal, sedeId);
                            return false;
                        })
                        .orElse(false))
                .orElseGet(() -> {
                    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                    if (auth != null && "n8n".equals(auth.getPrincipal())) {
                        return true;
                    }
                    return staffSecurityHelper.hasAnyRole(SEDE_ACCESS_ROLES_FALLBACK.toArray(new RoleName[0]));
                });
    }

    private boolean medicoHasSedeAccess(StaffPrincipal principal, Integer sedeId) {
        return principal.medicoId()
                .map(medicoId -> !scheduleRepository.findByDoctorIdAndSedeId(medicoId, sedeId).isEmpty())
                .orElse(false);
    }

    public void assertCurrentUserCanAccessSede(Integer sedeId) {
        if (!canAccessSede(sedeId)) {
            throw new AccessDeniedException("No tiene acceso a la sede: " + sedeId);
        }
    }
}
