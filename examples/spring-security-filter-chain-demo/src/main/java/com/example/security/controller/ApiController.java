package com.example.security.controller;

import com.example.security.service.GreetingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Protected API endpoints.
 *
 * These controllers are ONLY reached if the filter chain allowed the request through.
 * By the time we are here:
 *  - JwtAuthenticationFilter has already validated the token and populated SecurityContextHolder
 *  - AuthorizationFilter has already verified the user has ROLE_USER
 *
 * If either check failed, ExceptionTranslationFilter would have returned 401/403
 * and this code would never execute.
 */
@RestController
@RequestMapping("/api")
public class ApiController {

    private static final Logger log = LoggerFactory.getLogger(ApiController.class);

    private final GreetingService greetingService;

    public ApiController(GreetingService greetingService) {
        this.greetingService = greetingService;
    }

    /**
     * GET /api/hello — requires ROLE_USER (enforced by AuthorizationFilter before we get here).
     * Calls GreetingService which reads the principal from SecurityContextHolder.
     */
    @GetMapping("/hello")
    public Map<String, String> hello() {
        log.info("[ApiController] /api/hello reached — filter chain passed the request through");
        String message = greetingService.buildGreeting();
        return Map.of("message", message);
    }
}
