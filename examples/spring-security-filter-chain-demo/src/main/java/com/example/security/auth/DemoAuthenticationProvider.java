package com.example.security.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

/**
 * Custom AuthenticationProvider plugged into ProviderManager.
 *
 * Responsibilities:
 *   1. supports() — tells ProviderManager which token types this provider handles.
 *   2. authenticate() — loads UserDetails, validates credentials, and returns
 *      a fully-authenticated token that gets stored in SecurityContextHolder.
 *
 * For JWT pre-auth tokens the "credential" is the literal string
 * "jwt-pre-auth" — we skip password checking because the JWT filter already
 * validated the signature before calling AuthenticationManager.
 *
 * For HTTP Basic the real encoded password is compared.
 */
public class DemoAuthenticationProvider implements AuthenticationProvider {

    private static final Logger log = LoggerFactory.getLogger(DemoAuthenticationProvider.class);
    private static final String JWT_CREDENTIAL_MARKER = "jwt-pre-auth";

    private final DemoUserDetailsService userDetailsService;

    public DemoAuthenticationProvider(DemoUserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    @Override
    public Authentication authenticate(Authentication authentication)
            throws AuthenticationException {

        String username   = authentication.getName();
        String credential = authentication.getCredentials() == null
                ? "" : authentication.getCredentials().toString();

        log.info("[DemoAuthenticationProvider] Authenticating principal: '{}'", username);

        UserDetails userDetails;
        try {
            userDetails = userDetailsService.loadUserByUsername(username);
        } catch (UsernameNotFoundException e) {
            throw new BadCredentialsException("Unknown user: " + username);
        }

        if (JWT_CREDENTIAL_MARKER.equals(credential)) {
            // JWT path: signature already verified in JwtAuthFilter — trust the token.
            log.info("[DemoAuthenticationProvider] JWT pre-auth path — skipping password check");
        } else {
            // HTTP Basic / form-login path: verify the raw password.
            if (!userDetailsService.getEncoder().matches(credential, userDetails.getPassword())) {
                log.warn("[DemoAuthenticationProvider] Bad credentials for user: '{}'", username);
                throw new BadCredentialsException("Bad credentials");
            }
            log.info("[DemoAuthenticationProvider] Password match successful");
        }

        // Return a fully authenticated token — credentials are erased for safety.
        return new UsernamePasswordAuthenticationToken(
                userDetails,
                null,                       // erase credentials after auth
                userDetails.getAuthorities()
        );
    }

    @Override
    public boolean supports(Class<?> authentication) {
        // This provider handles the standard username+password token type,
        // which is what both JwtAuthFilter and HTTP Basic generate.
        return UsernamePasswordAuthenticationToken.class
                .isAssignableFrom(authentication);
    }
}
