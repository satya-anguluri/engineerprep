package com.example.security.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Handled by the PUBLIC SecurityFilterChain — no authentication required.
 */
@RestController
@RequestMapping("/public")
public class PublicController {

    @GetMapping("/hello")
    public Map<String, String> hello() {
        return Map.of(
            "message", "Hello from public endpoint — no auth required",
            "hint",    "This request matched the @Order(1) PUBLIC-Chain"
        );
    }
}
