package com.ipscentir.appointments.presentation.rest.config;

import com.ipscentir.appointments.infrastructure.security.StaffAccessDeniedHandler;
import com.ipscentir.appointments.infrastructure.security.StaffJwtAuthenticationEntryPoint;
import com.ipscentir.appointments.infrastructure.security.SupabaseJwtAuthenticationConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

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
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:3000", "http://127.0.0.1:3000"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/auth/supabase-config", "/api/v1/auth/refresh").permitAll()
                .requestMatchers("/api/v1/forms/**").permitAll()
                .requestMatchers("/api/v1/catalogs/**").permitAll()
                .requestMatchers("/api/v1/integrations/n8n/**").permitAll()
                .requestMatchers("/api/v1/admin/appointments/**")
                    .hasAnyRole("ADMINISTRACION", "ADMISIONES", "ASESOR")
                .requestMatchers("/api/v1/staff/patients/**")
                    .hasAnyRole("ADMINISTRACION", "ADMISIONES", "ASESOR")
                .requestMatchers("/api/v1/admin/**").hasRole("ADMINISTRACION")
                .requestMatchers(
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/v3/api-docs/**"
                ).permitAll()
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                .requestMatchers("/error").permitAll()
                .requestMatchers("/actuator/**").hasRole("ADMINISTRACION")
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth -> oauth
                    .authenticationEntryPoint(new StaffJwtAuthenticationEntryPoint())
                    .accessDeniedHandler(new StaffAccessDeniedHandler())
                    .jwt(jwt -> jwt.jwtAuthenticationConverter(supabaseJwtAuthenticationConverter)))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .addFilterBefore(n8nApiKeyFilter, BasicAuthenticationFilter.class);

        return http.build();
    }
}
