package com.example.security.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Public endpoint — no authentication required.
 *
 * Matched by the first SecurityFilterChain (Order=1, /public/**) which has
 * permitAll() for all requests, so no token is needed.
 */
@RestController
@RequestMapping("/public")
public class PublicController {

    private static final Logger log = LoggerFactory.getLogger(PublicController.class);

    @GetMapping("/info")
    public Map<String, String> info() {
        log.info("[PublicController] /public/info reached — no auth required");
        return Map.of(
            "message",     "This endpoint is public",
            "description", "Matched by SecurityFilterChain #1 (Order=1, /public/**)."
        );
    }
}
