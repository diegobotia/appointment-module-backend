package com.ipscentir.appointments.presentation.rest.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final N8nApiKeyFilter n8nApiKeyFilter;

    public SecurityConfig(N8nApiKeyFilter n8nApiKeyFilter) {
        this.n8nApiKeyFilter = n8nApiKeyFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable) // Disable CSRF for pure REST APIs
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/auth/login").permitAll()
                .requestMatchers("/api/v1/catalogs/**").permitAll()
                .requestMatchers("/api/v1/integrations/n8n/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/appointments").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/forms/pqrs").permitAll()
                .requestMatchers("/api/v1/admin/**").hasAnyRole("ADMIN", "SUPERADMIN")
                .requestMatchers(
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/v3/api-docs/**",
                        "/api-docs",
                    "/api-docs/**",
                    "/api/swagger-ui/**",
                    "/api/swagger-ui.html",
                    "/api/api-docs",
                    "/api/api-docs/**"
                ).permitAll() // OpenAPI Whitelist
                .anyRequest().authenticated()
            )
            .httpBasic(Customizer.withDefaults())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
            .addFilterBefore(n8nApiKeyFilter, BasicAuthenticationFilter.class);

        return http.build();
    }
}
