package com.example.security.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Admin-only endpoint.
 *
 * Requires ROLE_ADMIN — configured in SecurityConfig:
 *   .requestMatchers("/api/admin").hasRole("ADMIN")
 *
 * If a ROLE_USER (but not ROLE_ADMIN) user hits this endpoint:
 *   - AuthorizationFilter throws AccessDeniedException
 *   - ExceptionTranslationFilter catches it → 403 Forbidden
 *   - This controller method is NEVER invoked
 */
@RestController
@RequestMapping("/api")
public class AdminController {

    private static final Logger log = LoggerFactory.getLogger(AdminController.class);

    @GetMapping("/admin")
    public Map<String, String> adminArea(@AuthenticationPrincipal Object principal) {
        // @AuthenticationPrincipal is a convenience annotation that reads
        // SecurityContextHolder.getContext().getAuthentication().getPrincipal()
        String name = (principal instanceof org.springframework.security.core.Authentication auth)
            ? auth.getName()
            : String.valueOf(principal);

        log.info("[AdminController] /api/admin reached by '{}'", name);
        return Map.of("message", "Welcome to the admin area, " + name);
    }
}
