package com.borodkir.teamjob;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

@Component
public class JwtCore {
    @Value("${testing.app.secret}")
    private String secret;

    @Value("${testing.app.lifetime}")
    private int lifetime;

    private SecretKey key;

    /**
     * Generates a JWT token for the authenticated user.
     *
     * @param authentication the authentication object containing user details
     * @return the generated JWT token
     */
    public String generateToken(Authentication authentication) {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        LocalDateTime now = LocalDateTime.now();
        Date issuedAt = Date.from(now.atZone(ZoneId.systemDefault()).toInstant());
        Date expiration = Date.from(now.plusSeconds(lifetime).atZone(ZoneId.systemDefault()).toInstant());

        key = Keys.hmacShaKeyFor(secret.getBytes());

        return Jwts.builder()
                .subject(userDetails.getUsername())
                .issuedAt(issuedAt)
                .expiration(expiration)
                .signWith(key)
                .compact();
    }

    /**
     * Extracts the username (subject) from a given JWT token using the modern JJWT API.
     *
     * @param token the JWT token from which to extract the username
     * @return the username.
     */
    public String getNameFromJwtToken(String token) {
        key = Keys.hmacShaKeyFor(secret.getBytes());

        try {
            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .getSubject();
        } catch (Exception e) {
            System.err.println("Error parsing JWT token: " + e.getMessage());
            return null;
        }

    }

}