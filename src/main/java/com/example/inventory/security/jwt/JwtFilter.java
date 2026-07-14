package com.example.inventory.security.jwt;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.example.inventory.exceptions.auth.InvalidTokenTypeException;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {
    private final JwtAuthenticationProvider authenticationService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");
        String requestURI = request.getRequestURI();

        try {
            if (authHeader == null || authHeader.isBlank() || !authHeader.startsWith("Bearer ")) {
                log.debug("Inventory: no Bearer token found for URI: {}, skipping JWT authentication", requestURI);
                filterChain.doFilter(request, response);
                return;
            }

            String jwt = authHeader.substring(7);

            if (jwt.isBlank()) {
                log.warn("Inventory: Bearer header is present but token is blank for URI: {} from IP: {}", requestURI, request.getRemoteAddr());
                sendError(response, HttpServletResponse.SC_UNAUTHORIZED, "Invalid JWT Token in Bearer Header");
                return;
            }

            log.debug("Inventory: attempting JWT authentication for URI: {}", requestURI);
            Authentication authToken = authenticationService.authenticate(jwt);

            SecurityContextHolder.getContext().setAuthentication(authToken);
            log.debug("Inventory: successfully authenticated. Principal: {} loaded into SecurityContext for URI: {}",
                    authToken.getName(), requestURI);

            filterChain.doFilter(request, response);

        } catch (JWTVerificationException ex) {
            log.warn("Inventory: JWT verification failed for URI: {} - Reason: {}", requestURI, ex.getMessage());
            sendError(response, HttpServletResponse.SC_UNAUTHORIZED, "Invalid JWT Token");

        } catch (InvalidTokenTypeException ex) {
            log.warn("Inventory: token type mismatch for URI: {} - Reason: {}", requestURI, ex.getMessage());
            sendError(response, HttpServletResponse.SC_UNAUTHORIZED, "Token type is not allowed for this endpoint");

        } catch (IllegalArgumentException ex) {
            log.warn("Inventory: malformed token payload for URI: {} - Reason: {}", requestURI, ex.getMessage());
            sendError(response, HttpServletResponse.SC_UNAUTHORIZED, "Malformed token payload");

        } catch (EntityNotFoundException ex) {
            log.warn("Inventory: user/Device entity mapped to token not found in database for URI: {}", requestURI);
            sendError(response, HttpServletResponse.SC_NOT_FOUND, "User not found");

        } catch (Exception ex) {
            log.error("Inventory: unexpected security exception during JWT processing for URI: {}", requestURI, ex);
            sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error");
        }
    }

    private void sendError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.getWriter().write(message);
        response.getWriter().flush();
    }
}