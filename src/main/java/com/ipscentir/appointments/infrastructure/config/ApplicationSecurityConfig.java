package com.ipscentir.appointments.infrastructure.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
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
        MacAlgorithm macAlg = selectMacAlgorithm(keyBytes.length);
        SecretKey key = new SecretKeySpec(keyBytes, jcaAlgorithm(macAlg));
        return NimbusJwtDecoder.withSecretKey(key)
                .macAlgorithm(macAlg)
                .build();
    }

    private static MacAlgorithm selectMacAlgorithm(int keyLength) {
        if (keyLength >= 64) return MacAlgorithm.HS512;
        if (keyLength >= 48) return MacAlgorithm.HS384;
        return MacAlgorithm.HS256;
    }

    private static String jcaAlgorithm(MacAlgorithm macAlg) {
        if (macAlg == MacAlgorithm.HS512) return "HmacSHA512";
        if (macAlg == MacAlgorithm.HS384) return "HmacSHA384";
        return "HmacSHA256";
    }
}
