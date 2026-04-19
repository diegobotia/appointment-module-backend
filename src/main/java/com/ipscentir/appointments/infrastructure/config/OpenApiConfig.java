package com.ipscentir.appointments.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("API de Módulo de Citas - IPS Centir")
                        .version("1.0")
                        .description("Documentación de la API REST para el módulo de citas médicas de IPS Centir.")
                        .contact(new Contact()
                                .name("Equipo de Desarrollo")
                                .email("soporte@ipscentir.com"))
                        .license(new License().name("Privado").url("https://ipscentir.com")));
    }
}
