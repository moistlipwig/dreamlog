package pl.kalin.dreamlog.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

import lombok.RequiredArgsConstructor;
import pl.kalin.dreamlog.user.service.CustomUserDetailsService;
import pl.kalin.dreamlog.user.service.OAuth2SuccessHandler;
import pl.kalin.dreamlog.user.service.UserService;

/**
 * Spring Security configuration for DreamLog application.
 *
 * <p>Implements BFF (Backend For Frontend) pattern with:
 * <ul>
 *   <li>Session-based authentication (HttpOnly cookies)</li>
 *   <li>CSRF protection with double-submit cookie pattern</li>
 *   <li>Dual authentication: Form login (email/password) + OAuth2 (Google)</li>
 *   <li>JSON responses for API endpoints (no redirects)</li>
 * </ul>
 *
 * <p><b>Security Architecture:</b>
 * <ul>
 *   <li>Sessions stored server-side with JSESSIONID cookie (HttpOnly, Secure in prod)</li>
 *   <li>CSRF token in XSRF-TOKEN cookie (readable by JavaScript) + X-XSRF-TOKEN header</li>
 *   <li>Max 5 concurrent sessions per user</li>
 *   <li>Public endpoints: /api/auth/register, /api/auth/login, /api/auth/csrf, /oauth2/**</li>
 *   <li>Protected endpoints: /api/** (requires authentication)</li>
 * </ul>
 *
 * @see CustomUserDetailsService for form login authentication
 * @see OAuth2SuccessHandler for OAuth2 login handling
 */
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {
    private final CustomUserDetailsService userDetailsService;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Value("${app.frontend.oauth-success-path}")
    private String oauthSuccessPath;

    private static final String[] PUBLIC_ENDPOINTS = {
        "/v3/api-docs/**",
        "/swagger-ui/**",
        "/swagger-ui.html",
        "/actuator/health",
        "/error",
        "/api/auth/register",
        "/api/auth/login",
        "/oauth2/**",
        "/login/**"
    };

    @Bean
    public OAuth2SuccessHandler oAuth2SuccessHandler() {
        String redirectUrl = frontendUrl + oauthSuccessPath;
        return new OAuth2SuccessHandler(userService, redirectUrl);
    }


    /**
     * CSRF token repository with SameSite=Strict for enhanced security.
     * XSRF-TOKEN cookie is readable by JavaScript (httpOnly=false) but restricted to same-site requests.
     */
    @Bean
    public CookieCsrfTokenRepository csrfTokenRepository() {
        CookieCsrfTokenRepository repository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        repository.setCookieCustomizer(cookie -> cookie.sameSite("Strict"));
        return repository;
    }

    /**
     * AuthenticationManager bean for programmatic authentication in controllers.
     * Note: This creates a SEPARATE manager from the one .formLogin() uses internally,
     * but with identical configuration (same UserDetailsService + PasswordEncoder).
     */
    @Bean
    public AuthenticationManager authenticationManager() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provider);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // CSRF token handler for SPA - allows Angular to read XSRF-TOKEN cookie
        CsrfTokenRequestAttributeHandler requestHandler = new CsrfTokenRequestAttributeHandler();
        requestHandler.setCsrfRequestAttributeName("_csrf");

        http
            // Session management: server-side sessions for BFF pattern
            .sessionManagement(sm -> sm
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                .maximumSessions(5) // Limit concurrent sessions to prevent abuse
            )
            // CSRF protection: double-submit cookie pattern (cookie + header)
            .csrf(csrf -> csrf
                .csrfTokenRepository(csrfTokenRepository())
                .csrfTokenRequestHandler(requestHandler)
                .ignoringRequestMatchers("/login/oauth2/code/*")
                .ignoringRequestMatchers("/oauth2/*")
                .ignoringRequestMatchers("/api/auth/register") // First-time users can't have CSRF token yet
            )
            // Authorization: public endpoints vs. authenticated endpoints
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                .requestMatchers("/api/**").authenticated()
                .anyRequest().authenticated()
            )
            // Form login: email/password authentication
            .formLogin(form -> form
                .loginProcessingUrl("/api/auth/login")
                .successHandler(authenticationSuccessHandler())
                .failureHandler((request, response, exception) -> {
                    response.setStatus(401);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"error\":\"Invalid credentials\"}");
                })
                .permitAll()
            )
            // Link UserDetailsService for loading user credentials
            .userDetailsService(userDetailsService)
            // OAuth2 login: Google authentication
            .oauth2Login(oauth -> oauth
                .loginPage("/login")
                .successHandler(oAuth2SuccessHandler())
                .failureUrl("/login?error")
            )
            // Exception handling: JSON responses for API, redirects for HTML pages
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> {
                    if (request.getRequestURI().startsWith("/api/")) {
                        // API endpoints: return 401 JSON response
                        response.setStatus(401);
                        response.setContentType("application/json");
                        response.getWriter().write("{\"error\":\"Unauthorized\"}");
                    } else {
                        // HTML pages: redirect to login
                        response.sendRedirect("/login");
                    }
                })
            )
            // Logout: invalidate session and clear cookies
            .logout(logout -> logout
                .logoutUrl("/api/auth/logout")
                .logoutSuccessHandler(logoutSuccessHandler())
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID", "XSRF-TOKEN")
                .permitAll()
            )
            // SPA CSRF token filter: ensures token is loaded on every request
            // This prevents "first request fails, second succeeds" behavior in SPAs
            .addFilterAfter(new SpaCsrfTokenFilter(), CsrfFilter.class);

        return http.build();
    }

    /**
     * Custom success handler for form login.
     * Returns JSON response instead of redirect (for SPA compatibility).
     */
    @Bean
    public AuthenticationSuccessHandler authenticationSuccessHandler() {
        return (request, response, authentication) -> {
            response.setStatus(200);
            response.setContentType("application/json");
            response.getWriter().write("{\"success\":true}");
        };
    }

    /**
     * Custom success handler for logout.
     * Returns JSON response instead of redirect (for SPA compatibility).
     */
    @Bean
    public LogoutSuccessHandler logoutSuccessHandler() {
        return (request, response, authentication) -> {
            response.setStatus(200);
            response.setContentType("application/json");
            response.getWriter().write("{\"success\":true}");
        };
    }
}
