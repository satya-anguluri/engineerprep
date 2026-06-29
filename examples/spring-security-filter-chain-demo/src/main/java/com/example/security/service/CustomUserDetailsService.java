package com.example.security.service;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * In-memory UserDetailsService used by the AuthenticationManager.
 *
 * In a real app this would query a database.
 * Spring Security's DaoAuthenticationProvider calls loadUserByUsername()
 * during form-login / password authentication.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    // username → {password, roles}
    private static final Map<String, UserRecord> USERS = Map.of(
        "alice", new UserRecord("password", List.of("ROLE_USER")),
        "bob",   new UserRecord("password", List.of("ROLE_USER", "ROLE_ADMIN"))
    );

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserRecord record = USERS.get(username);
        if (record == null) {
            throw new UsernameNotFoundException("User not found: " + username);
        }
        List<SimpleGrantedAuthority> authorities = record.roles().stream()
            .map(SimpleGrantedAuthority::new)
            .toList();
        // Using {noop} prefix so Spring Security accepts plain-text passwords in the demo
        return new User(username, "{noop}" + record.password(), authorities);
    }

    private record UserRecord(String password, List<String> roles) {}
}
