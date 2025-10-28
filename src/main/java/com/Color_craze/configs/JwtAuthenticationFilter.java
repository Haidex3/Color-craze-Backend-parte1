package com.Color_craze.configs;

import java.io.IOException;

import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.Color_craze.auth.services.JwtService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        log.debug("→ Iniciando JwtAuthenticationFilter para: {}", request.getRequestURI());

        final String authHeader = request.getHeader("Authorization");

        // Si no hay token, dejamos pasar la request
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.debug("No hay token JWT, continuando cadena de filtros");
            filterChain.doFilter(request, response);
            return;
        }

        final String jwt = authHeader.substring(7);
        final String username = jwtService.extractUsername(jwt);

        // Solo autenticar si no hay autenticación previa
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);

            if (jwtService.isTokenValid(jwt, userDetails)) {
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities());
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authToken);
                log.debug("Autenticación JWT exitosa para usuario: {}", username);
            } else {
                log.debug("Token JWT inválido para usuario: {}", username);
            }
        } else if (SecurityContextHolder.getContext().getAuthentication() != null) {
            log.debug("Ya existe autenticación previa, saltando JWT filter");
        }

        // Continuar con la cadena de filtros
        filterChain.doFilter(request, response);
        log.debug("← Saliendo JwtAuthenticationFilter para: {}", request.getRequestURI());
    }
}
