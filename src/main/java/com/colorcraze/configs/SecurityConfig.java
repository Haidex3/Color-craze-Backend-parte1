package com.colorcraze.configs;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.colorcraze.configs.filters.FirebaseTokenFilter;
import com.colorcraze.configs.filters.LoggingContextFilter;


@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final FirebaseTokenFilter firebaseTokenFilter;
    private final LoggingContextFilter loggingContextFilter;

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(request -> {

                    var corsConfig = new org.springframework.web.cors.CorsConfiguration();
                    corsConfig.setAllowedOrigins(java.util.List.of("http://localhost:5173", "https://proud-plant-0b52ed10f.3.azurestaticapps.net", "https://black-glacier-051db390f.3.azurestaticapps.net"));
                    corsConfig.setAllowedMethods(java.util.List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
                    corsConfig.setAllowedHeaders(java.util.List.of("*"));
                    corsConfig.setExposedHeaders(java.util.List.of("X-Correlation-ID"));
                    corsConfig.setAllowCredentials(true);
                    return corsConfig;
                }))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/auth/**",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/api/games/**",
                                "/api/waiting-room/**",
                                "/color-craze/ws/**",
                                "/ws/**",
                                "/public/**"
                        ).permitAll()
                        .anyRequest().authenticated()
                )

                .addFilterBefore(loggingContextFilter, UsernamePasswordAuthenticationFilter.class)

                .addFilterAfter(firebaseTokenFilter, LoggingContextFilter.class)

                .httpBasic(httpBasic -> {});

        return http.build();
    }
}
