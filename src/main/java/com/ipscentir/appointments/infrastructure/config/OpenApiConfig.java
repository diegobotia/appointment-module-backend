package com.ipscentir.appointments.infrastructure.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Value("${server.port:8080}")
    private int serverPort;

    private static final String BEARER_SCHEME = "bearer-jwt";

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .servers(List.of(new Server()
                        .url("http://localhost:" + serverPort)
                        .description("Entorno local / desarrollo")))
                .components(new Components()
                        .addSecuritySchemes(BEARER_SCHEME, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT de Supabase Auth (panel interno)")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME))
                .info(new Info()
                        .title("API de Módulo de Citas - IPS Centir")
                        .version("1.1")
                        .description("""
                                API REST del módulo de citas médicas IPS Centir.

                                **Autenticación**
                                - Panel interno: `Authorization: Bearer <jwt_supabase>` (obtener token vía SDK Supabase).
                                - n8n / chat paciente: header `X-API-Key` en `/api/v1/integrations/n8n/**` (no usar JWT).

                                **Contratos clave (panel)**
                                - `AppointmentDTO` incluye `medicoDisplayName`, `patientDisplayName` y `administrative` en listados y detalle.
                                - Citas internas sin paciente: `POST /api/v1/appointments/administrative` → `appointmentType=STAFF`, `administrative=true`.
                                - Búsqueda paciente mostrador: `GET /api/v1/staff/patients/search`.
                                - Directorio médicos (plan trimestral): `GET /api/v1/admin/medicos`.

                                Referencia narrativa: `docs/flujos-api.md`. Swagger deshabilitado en perfil `prod`.
                                """)
                        .contact(new Contact()
                                .name("Equipo de Desarrollo")
                                .email("soporte@ipscentir.com"))
                        .license(new License().name("Privado").url("https://ipscentir.com")));
    }
}
