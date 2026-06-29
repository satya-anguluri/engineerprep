package com.example.security.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.GenericFilterBean;

import java.io.IOException;

/**
 * Logs each request as it enters the security filter chain.
 * Placed FIRST in the chain so it fires before any security processing.
 *
 * This makes the filter pipeline visible in the console — key for understanding
 * that security runs BEFORE DispatcherServlet.
 */
@Component
public class FilterChainLoggingFilter extends GenericFilterBean {

    private static final Logger log = LoggerFactory.getLogger(FilterChainLoggingFilter.class);

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest http = (HttpServletRequest) request;
        log.info("[FilterChainLoggingFilter] >>> Entering security filter chain: {} {}",
                 http.getMethod(), http.getRequestURI());
        log.info("[FilterChainLoggingFilter]     Authorization header present: {}",
                 http.getHeader("Authorization") != null);

        chain.doFilter(request, response);

        log.info("[FilterChainLoggingFilter] <<< Exiting security filter chain: {} {}",
                 http.getMethod(), http.getRequestURI());
    }
}
