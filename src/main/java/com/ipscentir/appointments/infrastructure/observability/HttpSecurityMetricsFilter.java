package com.ipscentir.appointments.infrastructure.observability;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(Ordered.LOWEST_PRECEDENCE)
@RequiredArgsConstructor
public class HttpSecurityMetricsFilter extends OncePerRequestFilter {

    private final AppointmentsMetrics appointmentsMetrics;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        filterChain.doFilter(request, response);
        int status = response.getStatus();
        if (status == HttpServletResponse.SC_UNAUTHORIZED) {
            appointmentsMetrics.recordUnauthorized();
        } else if (status == HttpServletResponse.SC_FORBIDDEN) {
            appointmentsMetrics.recordForbidden();
        }
    }
}
