package pl.kalin.dreamlog.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Filter that ensures CSRF token is loaded and available for Single Page Applications.
 *
 * <p>Problem: Spring Security generates CSRF tokens lazily (on first mutation request).
 * For SPAs, this causes the first POST/PUT/DELETE after login to fail with 403 (missing token).
 *
 * <p>Solution: This filter explicitly loads the CSRF token on EVERY request, ensuring:
 * <ul>
 *   <li>XSRF-TOKEN cookie is set immediately after login</li>
 *   <li>Angular can read the cookie and send X-XSRF-TOKEN header on subsequent requests</li>
 *   <li>No "first request fails, second succeeds" behavior</li>
 * </ul>
 *
 * <p>Technical details:
 * <ul>
 *   <li>Calls {@link CsrfToken#getToken()} to force token generation if not present</li>
 *   <li>Runs AFTER Spring Security's CSRF filter (which stores token in request attribute)</li>
 *   <li>Does nothing if CSRF token not present (e.g., public endpoints)</li>
 * </ul>
 *
 * @see CookieCsrfTokenRepository for cookie storage configuration
 * @see SecurityConfig#securityFilterChain for filter chain setup
 */
public class SpaCsrfTokenFilter extends OncePerRequestFilter {

    /**
     * Loads CSRF token if present in request attributes.
     * This triggers {@link CookieCsrfTokenRepository} to set XSRF-TOKEN cookie in response.
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {

        // Get CSRF token from request attribute (set by Spring Security's CSRF filter)
        CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());

        // Force token generation by calling getToken() - this ensures cookie is set
        if (csrfToken != null) {
            csrfToken.getToken();
        }

        filterChain.doFilter(request, response);
    }
}
