package com.example.security.controller;

import com.example.security.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Handles POST /auth/login.
 *
 * Flow:
 *   1. Client sends {username, password} JSON.
 *   2. We build a UsernamePasswordAuthenticationToken (unauthenticated).
 *   3. AuthenticationManager.authenticate() delegates to DaoAuthenticationProvider
 *      which calls CustomUserDetailsService.loadUserByUsername() and checks the password.
 *   4. On success, we generate a JWT and return it.
 *
 * The controller itself is AFTER the filter chain — it is only reached because
 * /auth/login is marked permitAll() in SecurityConfig.
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthenticationManager authenticationManager;

    public AuthController(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req) {
        log.info("[AuthController] Login attempt for user: {}", req.username());

        // Step 1: Create an unauthenticated token
        UsernamePasswordAuthenticationToken authToken =
            new UsernamePasswordAuthenticationToken(req.username(), req.password());

        // Step 2: Delegate to AuthenticationManager → DaoAuthenticationProvider
        //         This is exactly what UsernamePasswordAuthenticationFilter does
        //         during form-login, but here we do it programmatically.
        Authentication authentication = authenticationManager.authenticate(authToken);

        // Step 3: Build JWT from the authenticated principal's authorities
        List<String> roles = authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .toList();

        String jwt = JwtUtil.generateToken(authentication.getName(), roles);

        log.info("[AuthController] Login successful for '{}', roles={}",
                 authentication.getName(), roles);

        return ResponseEntity.ok(Map.of(
            "token",    jwt,
            "username", authentication.getName(),
            "roles",    roles
        ));
    }

    public record LoginRequest(String username, String password) {}
}
