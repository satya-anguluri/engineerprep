package com.example.security.config;

import com.example.security.filter.FilterChainLoggingFilter;
import com.example.security.filter.JwtAuthenticationFilter;
import com.example.security.service.CustomUserDetailsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Security configuration declaring TWO SecurityFilterChain beans.
 *
 * FilterChainProxy holds both chains and selects the first whose requestMatcher matches.
 *
 * Chain 1 (@Order(1))  – /public/**
 *   No authentication required. Demonstrates that static/public paths get a minimal chain.
 *
 * Chain 2 (@Order(2))  – everything else (/** including /api/** and /auth/**)
 *   Stateless JWT auth.  The JwtAuthenticationFilter sits BEFORE Spring's
 *   UsernamePasswordAuthenticationFilter in the ordered filter list.
 *
 * The AuthController (/auth/login) is also in chain 2 but marked permitAll so the
 *   login endpoint itself doesn't require a token.
 *
 * Key components wired here:
 *   - DaoAuthenticationProvider  → AuthenticationManager  (used by AuthController)
 *   - JwtAuthenticationFilter    → populates SecurityContextHolder
 *   - FilterChainLoggingFilter   → shows filter chain entry/exit in logs
 *   - ExceptionTranslationFilter → auto-configured; translates 401/403
 *   - AuthorizationFilter        → auto-configured; enforces .authorizeHttpRequests() rules
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    private final CustomUserDetailsService userDetailsService;
    private final FilterChainLoggingFilter loggingFilter;

    public SecurityConfig(CustomUserDetailsService userDetailsService,
                          FilterChainLoggingFilter loggingFilter) {
        this.userDetailsService = userDetailsService;
        this.loggingFilter = loggingFilter;
    }

    // ------------------------------------------------------------------
    // Chain 1: Public resources — no authentication needed
    // Order(1) means FilterChainProxy checks this chain FIRST.
    // ------------------------------------------------------------------
    @Bean
    @Order(1)
    public SecurityFilterChain publicChain(HttpSecurity http) throws Exception {
        log.info("[SecurityConfig] Configuring PUBLIC SecurityFilterChain (order=1) for /public/**");

        http
            .securityMatcher("/public/**")          // This chain owns /public/**
            .authorizeHttpRequests(auth ->
                auth.anyRequest().permitAll()        // Everything under /public is open
            )
            .sessionManagement(s ->
                s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .csrf(csrf -> csrf.disable());

        // Add logging filter so we can see this chain running
        http.addFilterBefore(loggingFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // ------------------------------------------------------------------
    // Chain 2: API + Auth endpoints — JWT authentication enforced
    // Order(2): evaluated if chain 1 did not match.
    // ------------------------------------------------------------------
    @Bean
    @Order(2)
    public SecurityFilterChain apiChain(HttpSecurity http) throws Exception {
        log.info("[SecurityConfig] Configuring API SecurityFilterChain (order=2) for /**");

        http
            // No securityMatcher → matches everything not caught by chain 1
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/auth/login").permitAll()   // login endpoint is open
                .requestMatchers("/api/admin").hasRole("ADMIN") // ROLE_ADMIN required
                .requestMatchers("/api/**").hasRole("USER")     // ROLE_USER required
                .anyRequest().authenticated()
            )
            .sessionManagement(s ->
                s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .csrf(csrf -> csrf.disable())
            // Plug in our JWT filter BEFORE the standard UsernamePasswordAuthenticationFilter.
            // Order within the chain matters: JWT runs first, populates SecurityContext,
            // then AuthorizationFilter (at the end) reads it to check access rules.
            .addFilterBefore(loggingFilter,          UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // ------------------------------------------------------------------
    // Beans
    // ------------------------------------------------------------------

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter();
    }

    /**
     * DaoAuthenticationProvider wires UserDetailsService + password encoder.
     * It is the AuthenticationProvider that AuthenticationManager delegates to.
     */
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        // {noop} prefix in stored passwords means no encoding — demo only
        return provider;
    }

    /**
     * Expose AuthenticationManager as a bean so AuthController can call
     * authenticationManager.authenticate(credentials) directly.
     */
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
