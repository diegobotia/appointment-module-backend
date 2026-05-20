package com.ipscentir.appointments.infrastructure.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
@Configuration
@RequiredArgsConstructor
public class ApplicationSecurityConfig {

    @Value("${supabase.url:https://project.supabase.co}")
    private String supabaseUrl;

    @Bean
    public JwtDecoder jwtDecoder() {
        String baseUrl = supabaseUrl.startsWith("http") ? supabaseUrl : "https://" + supabaseUrl;
        String jwkSetUri = baseUrl + "/auth/v1/keys";
        return NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
    }
}
