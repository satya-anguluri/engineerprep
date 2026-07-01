package com.example.security.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Handled by the API SecurityFilterChain (@Order 2) — JWT required.
 *
 * By the time execution reaches here:
 *   1. DelegatingFilterProxy → FilterChainProxy selected this chain.
 *   2. FilterChainLoggingFilter logged the request.
 *   3. JwtAuthFilter extracted the token and populated SecurityContextHolder.
 *   4. AuthorizationFilter checked the required authority.
 *   5. ExceptionTranslationFilter is waiting to catch any AccessDeniedException.
 */
@RestController
@RequestMapping("/api")
public class ApiController {

    /**
     * Accessible to any authenticated user (ROLE_USER or ROLE_ADMIN).
     * Requires header:  Authorization: Bearer demo-token-for-user
     *              or:  Authorization: Bearer demo-token-for-admin
     */
    @GetMapping("/profile")
    public Map<String, Object> profile() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        List<String> roles = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        return Map.of(
            "user",    auth.getName(),
            "roles",   roles,
            "message", "Authenticated via JWT filter chain (@Order 2)"
        );
    }

    /**
     * Accessible only to ROLE_ADMIN.
     * User token → 403 (AuthorizationFilter throws AccessDeniedException,
     *                   ExceptionTranslationFilter converts it to 403).
     * Admin token → 200.
     */
    @GetMapping("/admin-only")
    public Map<String, String> adminOnly() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return Map.of(
            "message", "You have ROLE_ADMIN — access granted",
            "user",    auth.getName()
        );
    }
}
