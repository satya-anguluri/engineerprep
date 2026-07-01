package com.example.security.config;

import com.example.security.auth.DemoAuthenticationProvider;
import com.example.security.auth.DemoUserDetailsService;
import com.example.security.filter.FilterChainLoggingFilter;
import com.example.security.filter.JwtAuthFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.List;

/**
 * Defines THREE independent SecurityFilterChain beans, each matching a
 * different URL pattern. Spring Security iterates them in @Order and the
 * FIRST matching chain wins — the others are never consulted.
 *
 * Chain 1  (@Order 1)  /public/**  → no auth required
 * Chain 2  (@Order 2)  /api/**     → stateless JWT authentication
 * Chain 3  (@Order 3)  /admin/**   → session-based HTTP Basic authentication
 *
 * This mirrors a real-world scenario where a REST API and a server-rendered
 * admin panel coexist with completely different security policies.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    // ---------------------------------------------------------------
    // Shared infrastructure beans
    // ---------------------------------------------------------------

    @Bean
    public DemoUserDetailsService userDetailsService() {
        return new DemoUserDetailsService();
    }

    @Bean
    public DemoAuthenticationProvider demoAuthenticationProvider(
            DemoUserDetailsService userDetailsService) {
        return new DemoAuthenticationProvider(userDetailsService);
    }

    /**
     * ProviderManager is Spring Security's default AuthenticationManager.
     * It delegates to a list of AuthenticationProviders and returns the
     * result of the first one that supports the token type.
     */
    @Bean
    public AuthenticationManager authenticationManager(
            DemoAuthenticationProvider provider) {
        return new ProviderManager(List.of(provider));
    }

    // ---------------------------------------------------------------
    // Chain 1 — Public URLs: /public/**
    // Matches first; permits everything; no filters added.
    // ---------------------------------------------------------------

    @Bean
    @Order(1)
    public SecurityFilterChain publicChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/public/**")          // only intercept /public/**
            .authorizeHttpRequests(auth -> auth
                .anyRequest().permitAll()            // no credentials needed
            )
            .csrf(AbstractHttpConfigurer::disable)
            // Add our observability filter so logs show which chain ran
            .addFilterBefore(
                new FilterChainLoggingFilter("PUBLIC-Chain"),
                UsernamePasswordAuthenticationFilter.class
            );
        return http.build();
    }

    // ---------------------------------------------------------------
    // Chain 2 — REST API: /api/**
    // Stateless JWT; no session; no CSRF.
    // The JwtAuthFilter replaces UsernamePasswordAuthenticationFilter.
    // ---------------------------------------------------------------

    @Bean
    @Order(2)
    public SecurityFilterChain apiChain(
            HttpSecurity http,
            AuthenticationManager authenticationManager) throws Exception {

        JwtAuthFilter jwtFilter = new JwtAuthFilter(authenticationManager);

        http
            .securityMatcher("/api/**")
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(sm ->
                sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .addFilterBefore(
                new FilterChainLoggingFilter("API-JWT-Chain"),
                UsernamePasswordAuthenticationFilter.class
            )
            // JwtAuthFilter runs where UsernamePasswordAuthenticationFilter
            // would normally sit, but we never instantiate the latter.
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/admin-only").hasRole("ADMIN")
                .anyRequest().authenticated()
            );
        return http.build();
    }

    // ---------------------------------------------------------------
    // Chain 3 — Admin panel: /admin/**
    // Session-based HTTP Basic (simulates form-login for curl simplicity).
    // ---------------------------------------------------------------

    @Bean
    @Order(3)
    public SecurityFilterChain adminChain(
            HttpSecurity http,
            DemoAuthenticationProvider provider) throws Exception {

        http
            .securityMatcher("/admin/**")
            .authenticationProvider(provider)       // wire our custom provider
            .addFilterBefore(
                new FilterChainLoggingFilter("ADMIN-Session-Chain"),
                UsernamePasswordAuthenticationFilter.class
            )
            .httpBasic(basic -> {})                 // enable HTTP Basic
            .authorizeHttpRequests(auth -> auth
                .anyRequest().hasRole("ADMIN")
            );
        return http.build();
    }
}
