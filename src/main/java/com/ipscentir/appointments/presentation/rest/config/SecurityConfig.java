package com.ipscentir.appointments.presentation.rest.config;

import com.ipscentir.appointments.infrastructure.security.SupabaseJwtAuthenticationConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final N8nApiKeyFilter n8nApiKeyFilter;
    private final SupabaseJwtAuthenticationConverter supabaseJwtAuthenticationConverter;

    public SecurityConfig(
            N8nApiKeyFilter n8nApiKeyFilter,
            SupabaseJwtAuthenticationConverter supabaseJwtAuthenticationConverter
    ) {
        this.n8nApiKeyFilter = n8nApiKeyFilter;
        this.supabaseJwtAuthenticationConverter = supabaseJwtAuthenticationConverter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/auth/supabase-config", "/api/v1/auth/refresh").permitAll()
                .requestMatchers("/api/v1/forms/**").permitAll()
                .requestMatchers("/api/v1/catalogs/**").permitAll()
                .requestMatchers("/api/v1/integrations/n8n/**").permitAll()
                .requestMatchers("/api/v1/admin/appointments/**")
                    .hasAnyRole("ADMINISTRACION", "ADMISIONES", "ASESOR")
                .requestMatchers("/api/v1/admin/**").hasRole("ADMINISTRACION")
                .requestMatchers(
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/v3/api-docs/**"
                ).permitAll()
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                .requestMatchers("/actuator/**").hasRole("ADMINISTRACION")
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth -> oauth
                    .jwt(jwt -> jwt.jwtAuthenticationConverter(supabaseJwtAuthenticationConverter)))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .addFilterBefore(n8nApiKeyFilter, BasicAuthenticationFilter.class);

        return http.build();
    }
}
