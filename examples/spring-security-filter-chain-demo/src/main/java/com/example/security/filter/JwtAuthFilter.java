package com.example.security.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Simulates a JWT bearer-token filter.
 *
 * Real-world flow:
 *   1. Extract the raw token from the Authorization header.
 *   2. Parse / verify the JWT signature → extract claims.
 *   3. Build a pre-authenticated token and store it in SecurityContextHolder.
 *
 * For demo simplicity the "token" is "demo-token-for-{username}" and no
 * cryptographic verification is done — the token format is enough to show
 * how the filter interacts with AuthenticationManager and SecurityContext.
 *
 * Key teaching points:
 * - The filter runs BEFORE the controller (servlet filter layer).
 * - On success it sets SecurityContextHolder so downstream code sees the
 *   authenticated principal.
 * - On failure it sends 401 immediately; the request never reaches the
 *   DispatcherServlet.
 */
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String TOKEN_PREFIX  = "demo-token-for-";

    private final AuthenticationManager authenticationManager;

    public JwtAuthFilter(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest  request,
            HttpServletResponse response,
            FilterChain         chain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            log.warn("[JwtAuthFilter] No Bearer token found — sending 401");
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED,
                    "Missing or malformed Authorization header");
            return;
            // Note: we do NOT call chain.doFilter() — the request stops here.
        }

        String token = authHeader.substring(BEARER_PREFIX.length()).trim();
        log.info("[JwtAuthFilter] Extracted token: {}", token);

        if (!token.startsWith(TOKEN_PREFIX)) {
            log.warn("[JwtAuthFilter] Unrecognised token format — sending 401");
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid token");
            return;
        }

        // Parse the "username" from our demo token format.
        String username = token.substring(TOKEN_PREFIX.length());

        try {
            // Delegate to AuthenticationManager (ProviderManager) which calls
            // our DemoAuthenticationProvider.  This is the same pathway that
            // UsernamePasswordAuthenticationFilter uses.
            Authentication authRequest =
                new UsernamePasswordAuthenticationToken(username, "jwt-pre-auth");

            Authentication authResult = authenticationManager.authenticate(authRequest);

            // SUCCESS: store the fully-populated Authentication in the
            // SecurityContextHolder (ThreadLocal by default).
            SecurityContextHolder.getContext().setAuthentication(authResult);
            log.info("[JwtAuthFilter] Authentication successful — principal: {}, authorities: {}",
                    authResult.getName(), authResult.getAuthorities());

            // Continue the filter chain — next stop: AuthorizationFilter,
            // then (if authorised) DispatcherServlet.
            chain.doFilter(request, response);

        } catch (AuthenticationException ex) {
            log.warn("[JwtAuthFilter] Authentication failed: {}", ex.getMessage());
            // Clear any partial context to avoid leaking state.
            SecurityContextHolder.clearContext();
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, ex.getMessage());
        }
    }
}
