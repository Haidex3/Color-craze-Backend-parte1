package com.colorcraze.utils;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;

/**
 * Utility class for generating JSON Web Tokens (JWT) for authentication.
 * 
 * This component handles the creation of JWTs with a subject, role, issued date,
 * expiration date, and a unique identifier. The tokens are signed using HMAC SHA.
 */
@Component
public class JwtUtil {

    private final SecretKey secretKey;
    private final long expirationMs;

    /**
     * Constructs the JwtUtil with the secret key and expiration time.
     * 
     * @param secret       The secret key used to sign JWTs, injected from application properties.
     * @param expirationMs Token expiration time in milliseconds, defaults to 3600000 (1 hour) if not set.
     */
    public JwtUtil(@Value("${app.jwt.secret}") String secret,
                @Value("${app.jwt.expiration-ms:3600000}") long expirationMs) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes());
        this.expirationMs = expirationMs;
    }

    /**
     * Generates a JWT for a given subject and role.
     * 
     * @param subject The subject (usually username or user ID) for whom the token is generated.
     * @param role    The role or authority of the subject to include as a claim in the token.
     * @return A signed JWT as a String.
     */
    public String generateToken(String subject, String role) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .header()
                    .type("JWT")
                    .and() 
                .id(UUID.randomUUID().toString())
                .subject(subject)
                .claim("role", role)
                .issuedAt(now)
                .expiration(exp)
                .signWith(secretKey, Jwts.SIG.HS256)
                .compact();
    }

}
