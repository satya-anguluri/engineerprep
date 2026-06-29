package com.example.security.filter;

import com.example.security.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * JWT Bearer Token Authentication Filter.
 *
 * Analogous to Spring Security's BearerTokenAuthenticationFilter.
 *
 * Pipeline:
 *   1. Extract "Authorization: Bearer <token>" header.
 *   2. Validate & parse the JWT via JwtUtil.
 *   3. Build an Authentication object and place it in SecurityContextHolder.
 *   4. Downstream AuthorizationFilter reads SecurityContextHolder to enforce access rules.
 *
 * If the token is missing or invalid, this filter does nothing — ExceptionTranslationFilter
 * will catch the resulting AccessDeniedException and return 401/403.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.debug("[JwtAuthenticationFilter] No Bearer token found, skipping.");
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        try {
            Map<String, String> claims = JwtUtil.validateAndParse(token);
            String username = claims.get("sub");
            String rolesStr = claims.getOrDefault("roles", "");

            List<SimpleGrantedAuthority> authorities = Arrays.stream(rolesStr.split(","))
                .filter(r -> !r.isBlank())
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());

            // ---------------------------------------------------------------
            // KEY STEP: Populate SecurityContextHolder with the authenticated
            // principal.  AuthorizationFilter will read this later in the chain.
            // ---------------------------------------------------------------
            UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(username, null, authorities);

            SecurityContextHolder.getContext().setAuthentication(authentication);

            log.info("[JwtAuthenticationFilter] Authenticated '{}' with roles: {}",
                     username, authorities);

        } catch (Exception e) {
            log.warn("[JwtAuthenticationFilter] Token validation failed: {}", e.getMessage());
            // Clear any partial context and let ExceptionTranslationFilter handle it
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}
