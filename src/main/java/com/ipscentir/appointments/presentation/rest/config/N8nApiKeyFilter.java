package com.ipscentir.appointments.presentation.rest.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

@Component
public class N8nApiKeyFilter extends OncePerRequestFilter {

    public static final String N8N_API_KEY_HEADER = "X-API-Key";

    private final byte[] configuredApiKey;
    private final String n8nBasePath;

    public N8nApiKeyFilter(
            @Value("${security.n8n.api-key:}") String configuredApiKey,
            @Value("${security.n8n.base-path:/api/v1/integrations/n8n}") String n8nBasePath
    ) {
        this.configuredApiKey = configuredApiKey.getBytes(StandardCharsets.UTF_8);
        this.n8nBasePath = n8nBasePath;
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        return !request.getRequestURI().startsWith(n8nBasePath);
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        String providedApiKey = request.getHeader(N8N_API_KEY_HEADER);
        byte[] providedApiKeyBytes = providedApiKey == null
                ? new byte[0]
                : providedApiKey.getBytes(StandardCharsets.UTF_8);

        if (!isApiKeyValid(providedApiKeyBytes)) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid or missing n8n API key");
            return;
        }

        UsernamePasswordAuthenticationToken auth =
                UsernamePasswordAuthenticationToken.authenticated("n8n", null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);

        try {
            filterChain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private boolean isApiKeyValid(byte[] providedApiKeyBytes) {
        if (configuredApiKey.length == 0 || providedApiKeyBytes.length == 0) {
            return false;
        }
        return MessageDigest.isEqual(configuredApiKey, providedApiKeyBytes);
    }
}