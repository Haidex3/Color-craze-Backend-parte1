package com.Color_craze.auth.services;

import java.util.Date;
import java.util.Map;
import java.util.function.Function;

import javax.crypto.SecretKey;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

/**
 * Service for handling JWT (JSON Web Token) operations such as
 * generating, validating, and extracting claims from tokens.
 */
@Service
public class JwtService {

    //@Value("${jwt.security}")
    //private String secretKey;
    private String secretKey ="EPRiC0Bt0P2KcBRRWqVKhEWzModEtI6Q4K05RWuLgVQV4Xw92Ulk9kHPmQVjiRW5c9XtLNm4lgNoridiLgvZpgC5";
    //@Value("${jwt.expiration}")
    //private long jwtExpiration;
    private long refreshExpiration = 3600000;

    private long jwtExpiration = 1800000;
    

    /**
     * Extracts the username (subject) from a JWT token.
     *
     * @param token The JWT token.
     * @return The username contained in the token.
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Generates a JWT token for a given user.
     *
     * @param userDetails The user details.
     * @return A signed JWT token.
     */
    public String generateToken(UserDetails userDetails) {
        return buildToken(Map.of(), userDetails, jwtExpiration);
    }

    /**
     * Extracts a specific claim from the JWT token using a claims resolver function.
     *
     * @param <T> The type of the claim to extract.
     * @param token The JWT token.
     * @param claimsResolver Function to extract the desired claim from Claims.
     * @return The value of the requested claim.
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Validates a JWT token by checking the username and expiration.
     *
     * @param token The JWT token.
     * @param userDetails The user details to compare against.
     * @return True if the token is valid, false otherwise.
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            if (userDetails == null) {
                return false;
            }
            final Claims claims = extractAllClaims(token);
            final String username = claims.getSubject();
            return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Builds a JWT token with custom claims, user details, and expiration time.
     *
     * @param extraClaims Additional claims to include in the token.
     * @param userDetails User details for the token subject.
     * @param expiration Token expiration in milliseconds.
     * @return A signed JWT token.
     */
    private String buildToken(
            Map<String, Object> extraClaims,
            UserDetails userDetails,
            long expiration) {
        return Jwts.builder()
                .claims(extraClaims)
                .subject(userDetails.getUsername())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSignInKey(), Jwts.SIG.HS256)
                .compact();
    }

    /**
     * Extracts all claims from the JWT token.
     *
     * @param token The JWT token.
     * @return The Claims object extracted from the token.
     * @throws JwtException If the token algorithm is invalid or parsing fails.
     */
    private Claims extractAllClaims(String token) {
        Jws<Claims> jws = Jwts.parser()
            .verifyWith(getSignInKey())
            .build()
            .parseSignedClaims(token);
        
        if (!"HS256".equals(jws.getHeader().getAlgorithm())) {
            throw new JwtException("Invalid JWT algorithm. Expected: HS256");
        }
        
        return jws.getPayload();
    }

    /**
     * Checks if a JWT token is expired.
     *
     * @param token The JWT token.
     * @return True if the token is expired, false otherwise.
     */
    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    /**
     * Extracts the expiration date from a JWT token.
     *
     * @param token The JWT token.
     * @return The expiration date.
     */
    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Retrieves the signing key for JWT tokens.
     *
     * @return A SecretKey used to sign tokens.
     */
    private SecretKey getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Generates a refresh token for a given user.
     * Includes the user's role in the token claims.
     *
     * @param userDetails The user details.
     * @return A signed refresh JWT token.
     */
    public String generateRefreshToken(UserDetails userDetails) {
        Map<String, Object> claims = Map.of(
            "role", userDetails.getAuthorities().stream().findFirst().get().getAuthority()
        );
        return buildToken(claims, userDetails, refreshExpiration);
    }

}