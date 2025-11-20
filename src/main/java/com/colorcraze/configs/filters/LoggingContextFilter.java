package com.colorcraze.configs.filters;

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
    private static final String HEADER_NAME = "X-Correlation-ID";

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String id = request.getHeader(HEADER_NAME);
        if (id == null || id.isBlank()) {
            id = UUID.randomUUID().toString();
        }

        MDC.put(CORRELATION_ID, id);

        response.setHeader(HEADER_NAME, id);

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();
        return path.startsWith("/ws")
                || path.startsWith("/color-craze/ws")
                || path.contains("/ws/info")
                || path.contains("/ws/connect")
                || path.endsWith("/websocket");
    }
}
