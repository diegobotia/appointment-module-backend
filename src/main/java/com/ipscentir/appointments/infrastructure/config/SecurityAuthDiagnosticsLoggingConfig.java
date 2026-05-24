package com.ipscentir.appointments.infrastructure.config;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "security.auth.diagnostics", havingValue = "true")
@Slf4j
public class SecurityAuthDiagnosticsLoggingConfig {

    @Value("${supabase.url:}")
    private String supabaseUrl;

    @PostConstruct
    void enableVerboseSecurityLogs() {
        setLevel("org.springframework.security", Level.DEBUG);
        setLevel("org.springframework.security.oauth2", Level.DEBUG);
        setLevel("com.ipscentir.appointments.infrastructure.security", Level.DEBUG);

        String base = supabaseUrl.startsWith("http") ? supabaseUrl : "https://" + supabaseUrl;
        log.warn("[AUTH-DIAGNOSTICS] Logs detallados activos. JWKS: {}/auth/v1/keys", base);
    }

    private static void setLevel(String loggerName, Level level) {
        Logger logger = (Logger) LoggerFactory.getLogger(loggerName);
        logger.setLevel(level);
    }
}
