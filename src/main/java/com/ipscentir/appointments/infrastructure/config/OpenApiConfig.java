package com.ipscentir.appointments.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Value("${server.port:8080}")
    private int serverPort;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .servers(List.of(new Server()
                        .url("http://localhost:" + serverPort)
                        .description("Entorno local / desarrollo")))
                .info(new Info()
                        .title("API de Módulo de Citas - IPS Centir")
                        .version("1.0")
                        .description("""
                                API REST del módulo de citas médicas IPS Centir.

                                - Prefijo de recursos: `/api/v1/...`
                                - Staff: `Authorization: Bearer <jwt_supabase>`
                                - n8n / chat: `X-API-Key` en `/api/v1/integrations/n8n/**`
                                - Swagger deshabilitado en perfil `prod`.
                                """)
                        .contact(new Contact()
                                .name("Equipo de Desarrollo")
                                .email("soporte@ipscentir.com"))
                        .license(new License().name("Privado").url("https://ipscentir.com")));
    }
}
