package com.example.inventory.security.jwt;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.example.inventory.exceptions.auth.InvalidTokenTypeException;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {
    private final JwtAuthenticationProvider authenticationService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");

        try {
            if (authHeader == null || authHeader.isBlank() || !authHeader.startsWith("Bearer ")) {
                filterChain.doFilter(request, response);
                return;
            }

            String jwt = authHeader.substring(7);

            if (jwt.isBlank()) {
                sendError(response, HttpServletResponse.SC_UNAUTHORIZED, "Invalid JWT Token in Bearer Header");
                return;
            }

            Authentication authToken = authenticationService.authenticate(jwt);

            SecurityContextHolder.getContext().setAuthentication(authToken);
            filterChain.doFilter(request, response);

        } catch (JWTVerificationException ex) {
            logger.error("JWT verification failed", ex);
            sendError(response, HttpServletResponse.SC_UNAUTHORIZED, "Invalid JWT Token");

        } catch (InvalidTokenTypeException ex) {
            logger.error("Token type mismatch", ex);
            sendError(response, HttpServletResponse.SC_UNAUTHORIZED, "Token type is not allowed for this endpoint");

        } catch (IllegalArgumentException ex) {
            logger.error("Malformed token payload", ex);
            sendError(response, HttpServletResponse.SC_UNAUTHORIZED, "Malformed token payload");

        } catch (EntityNotFoundException ex) {
            logger.error("User not found", ex);
            sendError(response, HttpServletResponse.SC_NOT_FOUND, "User not found");

        } catch (Exception ex) {
            logger.error("Authentication error", ex);
            sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error");
        }
    }

    private void sendError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.getWriter().write(message);
        response.getWriter().flush();
    }
}
