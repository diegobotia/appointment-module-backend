package com.ipscentir.appointments.infrastructure.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

@Configuration
@RequiredArgsConstructor
public class ApplicationSecurityConfig {

    @Value("${security.jwt.secret}")
    private String jwtSecret;

    @Bean
    public JwtDecoder jwtDecoder() {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        SecretKey key = new SecretKeySpec(keyBytes, hmacAlgorithm(keyBytes.length));
        return NimbusJwtDecoder.withSecretKey(key).build();
    }

    /**
     * Selecciona el algoritmo HMAC según el tamaño de la llave,
     * con la misma lógica que io.jsonwebtoken.security.Keys.hmacShaKeyFor()
     * en jjwt 0.11.5 (usado por el módulo de billing para firmar JWTs).
     */
    private static String hmacAlgorithm(int keyLength) {
        if (keyLength >= 64) return "HmacSHA512";
        if (keyLength >= 48) return "HmacSHA384";
        return "HmacSHA256";
    }
}
