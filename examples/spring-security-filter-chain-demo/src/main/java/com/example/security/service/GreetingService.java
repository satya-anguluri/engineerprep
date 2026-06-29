package com.example.security.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

/**
 * Business-layer service that reads the current principal from SecurityContextHolder.
 *
 * Demonstrates that ANY bean on the same thread can access the authenticated user
 * without it being passed explicitly — the ThreadLocal SecurityContext is the contract.
 */
@Service
public class GreetingService {

    private static final Logger log = LoggerFactory.getLogger(GreetingService.class);

    public String buildGreeting() {
        // ----------------------------------------------------------------
        // SecurityContextHolder.getContext().getAuthentication() is the
        // standard way to access the current principal from any Spring bean.
        // It works because JwtAuthenticationFilter populated the context
        // earlier in the same request thread.
        // ----------------------------------------------------------------
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()) {
            return "Hello, anonymous!";
        }

        String username = auth.getName();
        String roles = auth.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .collect(Collectors.joining(", ", "[", "]"));

        log.info("[GreetingService] Serving request for principal='{}' roles={}",
                 username, roles);

        return "Hello, " + username + "! Your roles: " + roles;
    }
}
