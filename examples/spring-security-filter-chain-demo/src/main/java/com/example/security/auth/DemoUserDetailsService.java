package com.example.security.auth;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;

/**
 * In-memory UserDetailsService used by our custom AuthenticationProvider.
 *
 * Users:
 *   user      / password   → ROLE_USER
 *   admin     / adminpass  → ROLE_ADMIN, ROLE_USER
 */
public class DemoUserDetailsService implements UserDetailsService {

    private final PasswordEncoder encoder =
        PasswordEncoderFactories.createDelegatingPasswordEncoder();

    private final Map<String, UserDetails> users;

    public DemoUserDetailsService() {
        users = Map.of(
            "user", User.builder()
                .username("user")
                .password(encoder.encode("password"))
                .roles("USER")
                .build(),
            "admin", User.builder()
                .username("admin")
                .password(encoder.encode("adminpass"))
                .roles("ADMIN", "USER")
                .build()
        );
    }

    @Override
    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException {
        UserDetails user = users.get(username);
        if (user == null) {
            throw new UsernameNotFoundException("User not found: " + username);
        }
        return user;
    }

    public PasswordEncoder getEncoder() {
        return encoder;
    }
}
