package com.borodkir.teamjob;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Component
public class TokenFilter extends OncePerRequestFilter {
    private final JwtCore jwtCore;
    private final UserDetailsService userDetailsService;
    private final List<String> PUBLIC_PATHS = Arrays.asList("/actuator/health", "/login", "/auth/", "/css/", "/js/", "/images/", "/signup", "/signin", "/signout", "/error");

    public TokenFilter(JwtCore jwtCore, UserDetailsService userDetailsService) {
        this.jwtCore = jwtCore;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();
        // Skip authentication for public paths
        if (isPublicPath(path)) {
            filterChain.doFilter(request, response);
            return;
        }
        System.out.println("\nTokenFilter: doFilterInternal() called" +
                "\nRequest path: " + path +
                "\nRequest method: " + request.getMethod() +
                "\nRequest headers: " + request.getHeaderNames() +
                "\nRequest cookies: " + Arrays.toString(request.getCookies()));
        try {
            String jwt = extractTokenFromHeader(request);
            if (jwt == null) {
                jwt = extractTokenFromCookies(request);
            }
            System.out.println("Extracted JWT: " + jwt);

            if (jwt != null && processToken(jwt)) {
                filterChain.doFilter(request, response);
                return;
            }

            System.out.println("JWT token is null or authentication failed");

            // Handle missing token
            handleAuthenticationFailure(request, response, "Missing JWT token");

        } catch (JwtException e) {
            // Handle invalid token
            System.out.println("Invalid JWT token: " + e.getMessage());
            handleAuthenticationFailure(request, response, "Invalid JWT token: " + e.getMessage());
        }
    }


    /**
     * Process the JWT token and set up authentication
     *
     * @return true if authentication was successful
     */
    private boolean processToken(String jwt) {
        String username = jwtCore.getNameFromJwtToken(jwt);

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    userDetails,
                    null,
                    userDetails.getAuthorities()
            );
            SecurityContextHolder.getContext().setAuthentication(auth);
            System.out.println("Authentication successful for user: " + username);
            return true;
        }
        System.out.println("Authentication failed for user: " + username);
        return false;
    }

    private void handleAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, String message)
            throws IOException {
        SecurityContextHolder.clearContext();

        // Determine if it's an API request or a browser request
        boolean isApiRequest = isApiRequest(request);

        if (isApiRequest) {
            // For API requests, return 401 status code
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write(message);
        } else {
            // For browser requests, redirect to login page
            response.sendRedirect("/signin");
        }
    }

    private boolean isApiRequest(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/api/") || request.getRequestURI().startsWith("/auth/");
    }

    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.stream().anyMatch(path::startsWith);
    }

    private String extractTokenFromHeader(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");
        if (headerAuth != null && headerAuth.startsWith("Bearer ")) {
            return headerAuth.substring(7);
        }
        return null;
    }

    private String extractTokenFromCookies(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("jwt".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}