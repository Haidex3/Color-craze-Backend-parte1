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

/**
 * Servlet filter that manages a correlation ID for each HTTP request.
 * The correlation ID is added to the MDC for logging and returned in the response header.
 * Certain WebSocket-related paths are excluded from filtering.
 */
@Component
public class LoggingContextFilter extends OncePerRequestFilter {

    private static final String CORRELATION_ID = "correlationId";
    private static final String HEADER_NAME = "X-Correlation-ID";

    /**
     * Filters incoming HTTP requests to set a correlation ID in MDC and response headers.
     * Generates a new UUID if the header is missing or blank.
     *
     * @param request the HTTP request
     * @param response the HTTP response
     * @param filterChain the filter chain
     * @throws ServletException if a servlet error occurs
     * @throws IOException if an I/O error occurs
     */
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

    /**
     * Determines whether the filter should not be applied for specific WebSocket-related paths.
     *
     * @param request the HTTP request
     * @return true if the request should be excluded from filtering; false otherwise
     */
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
