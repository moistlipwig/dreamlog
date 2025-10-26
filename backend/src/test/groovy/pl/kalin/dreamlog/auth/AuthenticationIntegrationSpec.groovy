package pl.kalin.dreamlog.auth

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.util.LinkedMultiValueMap
import pl.kalin.dreamlog.IntegrationSpec
import pl.kalin.dreamlog.support.SessionRestClient
import pl.kalin.dreamlog.user.UserRepository
import pl.kalin.dreamlog.user.dto.RegisterRequest
import pl.kalin.dreamlog.user.dto.SetPasswordRequest

/**
 * Integration tests for authentication flows (register, login, session, CSRF).
 * Uses AuthClient helper to manage cookies and CSRF automatically (KISS principle).
 */
class AuthenticationIntegrationSpec extends IntegrationSpec {

    @LocalServerPort
    int port

    @Autowired
    TestRestTemplate restTemplate

    @Autowired
    UserRepository userRepository

    AuthClient auth

    def setup() {
        userRepository.deleteAll()
        auth = new AuthClient(restTemplate, baseUrl())
    }

    String baseUrl() {
        "http://localhost:${port}"
    }

    // ============================================================================
    // Manual Registration Tests
    // ============================================================================

    def "should register user with email and password"() {
        when: "registering a new user"
        def response = auth.register("test@example.com", "SecurePassword123", "Test User")

        then: "user is created successfully"
        response.statusCode == HttpStatus.OK
        response.body.email == "test@example.com"
        response.body.name == "Test User"
        response.body.emailVerified == false
        response.body.hasPassword == true
        response.body.providers == []
    }


    // ============================================================================
    // Login Tests
    // ============================================================================

    def "should login with email and password"() {
        given: "a registered user"
        auth.register("login@example.com", "MyPassword123", "Login User")

        when: "logging in with correct credentials"
        def response = auth.login("login@example.com", "MyPassword123")

        then: "login is successful and session is created"
        response.statusCode == HttpStatus.OK
        response.body.success == true
        CookieAssertions.assertCookieExists(response, "JSESSIONID")
    }

    def "should reject invalid password"() {
        given: "a registered user"
        auth.register("wrong@example.com", "CorrectPassword123", "User")

        when: "trying to login with wrong password"
        def response = auth.login("wrong@example.com", "WrongPassword")

        then: "login is rejected"
        response.statusCode == HttpStatus.UNAUTHORIZED
        response.body.error == "Invalid credentials"
    }

    def "should reject login for nonexistent user"() {
        when: "trying to login as nonexistent user"
        def response = auth.login("nonexistent@example.com", "AnyPassword123")

        then: "login is rejected"
        response.statusCode == HttpStatus.UNAUTHORIZED
    }

    // ============================================================================
    // /api/me Endpoint Tests
    // ============================================================================

    def "should return user info when authenticated"() {
        given: "a registered and logged in user"
        auth.register("me@example.com", "MyPassword123", "Me User")
        auth.login("me@example.com", "MyPassword123")

        when: "calling /api/me as authenticated user"
        def response = auth.me()

        then: "user info is returned"
        response.statusCode == HttpStatus.OK
        response.body.email == "me@example.com"
        response.body.name == "Me User"
        response.body.hasPassword == true
    }

    def "should return 401 when not authenticated"() {
        when: "calling /api/me without authentication"
        def response = auth.me()

        then: "request is rejected with 401"
        response.statusCode == HttpStatus.UNAUTHORIZED
    }

    // ============================================================================
    // Session & CSRF Tests
    // ============================================================================

    def "should validate CSRF token on login"() {
        given: "a registered user"
        auth.register("csrf@example.com", "Password123", "CSRF User")

        when: "trying to login without CSRF token"
        def loginForm = new LinkedMultiValueMap<String, String>()
        loginForm.add("username", "csrf@example.com")
        loginForm.add("password", "Password123")

        def headers = new HttpHeaders()
        headers.contentType = MediaType.APPLICATION_FORM_URLENCODED

        def loginRequest = new HttpEntity<>(loginForm, headers)
        def responseWithoutCsrf = restTemplate.exchange(
            "${baseUrl()}/api/auth/login",
            HttpMethod.POST,
            loginRequest,
            Map
        )

        then: "request is forbidden"
        responseWithoutCsrf.statusCode == HttpStatus.FORBIDDEN

        when: "logging in with CSRF token (via AuthClient)"
        def responseWithCsrf = auth.login("csrf@example.com", "Password123")

        then: "login is successful"
        responseWithCsrf.statusCode == HttpStatus.OK
    }

    // ============================================================================
    // Account Linking Tests - Set Password
    // ============================================================================

    def "should allow authenticated user to set password"() {
        given: "a registered and logged in user"
        auth.register("oauth-only@example.com", "InitialPassword123", "OAuth User")
        auth.login("oauth-only@example.com", "InitialPassword123")

        when: "setting new password as authenticated user"
        def response = auth.setPassword("NewPassword123")

        then: "password is set successfully"
        response.statusCode == HttpStatus.OK
        response.body.success == true
    }

    def "should require authentication for set password"() {
        given: "a fresh client without authentication"
        auth.reset() // Clear any cookies

        when: "trying to set password without authentication"
        def response = auth.setPassword("NewPassword123")

        then: "request is rejected"
        response.statusCode == HttpStatus.UNAUTHORIZED
    }

    // ============================================================================
    // Helper Classes
    // ============================================================================

    /**
     * Mini-SDK for authentication operations.
     * Automatically manages cookies and CSRF tokens (KISS principle).
     */
    static class AuthClient extends SessionRestClient {

        AuthClient(TestRestTemplate rest, String baseUrl) {
            super(rest, baseUrl)
        }

        void reset() {
            cookieStore().clear()
        }

        ResponseEntity<Map> register(String email, String password, String name) {
            def request = new RegisterRequest(email, password, name)
            return json(HttpMethod.POST, "/api/auth/register", request, Map)
        }

        ResponseEntity<Map> login(String email, String password) {
            def loginForm = new LinkedMultiValueMap<String, String>()
            loginForm.add("username", email)
            loginForm.add("password", password)

            return submitForm("/api/auth/login", loginForm, Map)
        }

        ResponseEntity<Map> me() {
            return get("/api/me", Map)
        }

        Map csrf() {
            def response = get("/api/me", Map)
            return [
                token     : cookieStore().getCookies().find { it.name == "XSRF-TOKEN" }?.value,
                headerName: "X-XSRF-TOKEN",
                response  : response
            ]
        }

        ResponseEntity<Map> setPassword(String newPassword) {
            def passwordRequest = new SetPasswordRequest(newPassword)
            return json(HttpMethod.POST, "/api/auth/set-password", passwordRequest, Map)
        }
    }

    /**
     * Helper for cookie security assertions.
     * Validates HttpOnly, Secure, SameSite, Path attributes.
     */
    static class CookieAssertions {
        static void assertCookieExists(ResponseEntity<?> response, String cookieName) {
            def setCookieHeaders = response.headers.get(HttpHeaders.SET_COOKIE)
            def cookie = setCookieHeaders?.find { it.startsWith("${cookieName}=") }
            assert cookie != null: "Cookie '${cookieName}' not found in response"
        }

        static void assertCookie(ResponseEntity<?> response, String cookieName, Map<String, Object> expectations) {
            def setCookieHeaders = response.headers.get(HttpHeaders.SET_COOKIE)
            def cookie = setCookieHeaders?.find { it.startsWith("${cookieName}=") }
            assert cookie != null: "Cookie '${cookieName}' not found in response"

            if (expectations.httpOnly != null) {
                if (expectations.httpOnly) {
                    assert cookie.contains("HttpOnly"): "Cookie '${cookieName}' should be HttpOnly"
                } else {
                    assert !cookie.contains("HttpOnly"): "Cookie '${cookieName}' should NOT be HttpOnly"
                }
            }

            if (expectations.secure != null) {
                if (expectations.secure) {
                    assert cookie.contains("Secure"): "Cookie '${cookieName}' should be Secure"
                } else {
                    assert !cookie.contains("Secure"): "Cookie '${cookieName}' should NOT be Secure"
                }
            }

            if (expectations.sameSite) {
                assert cookie.contains("SameSite=${expectations.sameSite}"):
                    "Cookie '${cookieName}' should have SameSite=${expectations.sameSite}, but was: ${cookie}"
            }

            if (expectations.path) {
                assert cookie.contains("Path=${expectations.path}"):
                    "Cookie '${cookieName}' should have Path=${expectations.path}, but was: ${cookie}"
            }
        }
    }
}
