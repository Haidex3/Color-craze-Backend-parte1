package com.Color_craze.configs.filters;

import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class LoggingContextFilter extends OncePerRequestFilter {

    private static final String CORRELATION_ID = "correlationId";

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        // Assings an ID from the reques or generate a new one
        String id = request.getHeader("X-Correlation-ID");
        if (id == null) {
            id = UUID.randomUUID().toString();
        }

        MDC.put(CORRELATION_ID, id);
        response.setHeader("X-Correlation-ID", id);

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }

}