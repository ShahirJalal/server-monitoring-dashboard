package dev.shahirjalal.backend.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Spring Security's CSRF token is resolved lazily by default (only if something
 * reads it, e.g. a server-rendered form) -- with a pure JSON API nothing ever does,
 * so the XSRF-TOKEN cookie would never actually get written to the response.
 * Forcing {@code getToken()} here on every request guarantees the cookie is set
 * before Angular's HttpClient needs to read it back. This is Spring Security's
 * documented pattern for a cookie-based CSRF setup with a JS SPA.
 */
public class CsrfCookieFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        if (csrfToken != null) {
            csrfToken.getToken();
        }

        filterChain.doFilter(request, response);
    }
}
