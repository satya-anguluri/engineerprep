package com.example.security.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * A diagnostic filter added to every SecurityFilterChain.
 *
 * It logs:
 *   - BEFORE the rest of the chain: which chain matched and the raw request.
 *   - AFTER  the rest of the chain: the HTTP status and resulting principal.
 *
 * This makes it easy to observe the "first-wins" matching and see exactly
 * which filter chain handled each request.
 */
public class FilterChainLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(FilterChainLoggingFilter.class);

    private final String chainName;

    public FilterChainLoggingFilter(String chainName) {
        this.chainName = chainName;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest  request,
            HttpServletResponse response,
            FilterChain         chain) throws ServletException, IOException {

        log.info("[FilterChainLoggingFilter] >>> REQUEST: {} {}  matched chain: {}",
                request.getMethod(), request.getRequestURI(), chainName);

        // Let the rest of the security filter chain (and ultimately the
        // controller) run.
        chain.doFilter(request, response);

        // Post-processing: by now the SecurityContext is populated (or not).
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String principal = (auth != null && auth.isAuthenticated())
                ? auth.getName() + " " + auth.getAuthorities()
                : "<unauthenticated>";

        log.info("[FilterChainLoggingFilter] <<< RESPONSE: {}  chain: {}  principal: {}",
                response.getStatus(), chainName, principal);
    }
}
