package com.example.security.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Handled by the ADMIN SecurityFilterChain (@Order 3) — HTTP Basic required,
 * ROLE_ADMIN required.
 *
 * Try:
 *   curl -u admin:adminpass http://localhost:8080/admin/dashboard  → 200
 *   curl -u user:password   http://localhost:8080/admin/dashboard  → 403
 *   curl                    http://localhost:8080/admin/dashboard  → 401
 */
@RestController
@RequestMapping("/admin")
public class AdminController {

    @GetMapping("/dashboard")
    public Map<String, String> dashboard() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return Map.of(
            "message", "Welcome to admin dashboard (session-based, @Order 3 chain)",
            "user",    auth.getName()
        );
    }
}
