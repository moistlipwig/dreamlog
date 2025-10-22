package pl.kalin.dreamlog.auth

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.transaction.annotation.Transactional
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import pl.kalin.dreamlog.IntegrationSpec
import pl.kalin.dreamlog.user.UserRepository
import pl.kalin.dreamlog.user.dto.RegisterRequest
import pl.kalin.dreamlog.user.dto.SetPasswordRequest

@Transactional
class AuthenticationIntegrationSpec extends IntegrationSpec {

    @LocalServerPort
    int port

    @Autowired
    TestRestTemplate restTemplate

    @Autowired
    ObjectMapper objectMapper

    @Autowired
    UserRepository userRepository

    String baseUrl() {
        "http://localhost:${port}"
    }

    def setup() {
        userRepository.deleteAll()
    }

    /**
     * Gets CSRF token from server for use in POST requests.
     */
    Map<String, String> getCsrfToken() {
        def response = restTemplate.getForEntity("${baseUrl()}/api/auth/csrf", Map)
        def token = response.body.token as String
        def headerName = response.body.headerName as String
        return [token: token, headerName: headerName, cookie: response.headers.getFirst(HttpHeaders.SET_COOKIE)]
    }

    /**
     * Helper to perform login with CSRF token.
     */
    ResponseEntity<Map> performLogin(String username, String password) {
        def csrf = getCsrfToken()
        def loginForm = new LinkedMultiValueMap<String, String>()
        loginForm.add("username", username)
        loginForm.add("password", password)

        def headers = new HttpHeaders()
        headers.contentType = MediaType.APPLICATION_FORM_URLENCODED
        if (csrf.headerName && csrf.token) {
            headers.set(csrf.headerName, csrf.token)
        }
        if (csrf.cookie) {
            headers.set(HttpHeaders.COOKIE, csrf.cookie)
        }

        def loginRequest = new HttpEntity<MultiValueMap<String, String>>(loginForm, headers)
        return restTemplate.exchange(
            "${baseUrl()}/api/auth/login",
            HttpMethod.POST,
            loginRequest,
            Map
        )
    }

    /**
     * Extracts CSRF token value from Set-Cookie header.
     */
    String extractCsrfTokenFromCookie(List<String> setCookieHeaders) {
        def csrfCookie = setCookieHeaders?.find { it.startsWith("XSRF-TOKEN=") }
        if (csrfCookie) {
            def tokenPart = csrfCookie.split(";")[0] // Get "XSRF-TOKEN=value" part
            def parts = tokenPart.split("=", 2) // Split only on first "=" to handle value with "="
            return parts.length > 1 ? parts[1] : null
        }
        return null
    }

    /**
     * Combines all cookies from Set-Cookie headers into single Cookie header value.
     */
    String combineCookies(List<String> setCookieHeaders) {
        if (!setCookieHeaders) return null
        return setCookieHeaders.collect { cookieHeader ->
            cookieHeader.split(";")[0].trim() // Get "NAME=value" part and trim whitespace
        }.join("; ")
    }

    /**
     * Helper to perform logout with CSRF and session cookie.
     * Gets fresh CSRF token using existing session before logging out.
     */
    ResponseEntity<Map> performLogout(ResponseEntity loginResponse) {
        def setCookies = loginResponse.headers.get(HttpHeaders.SET_COOKIE)
        assert setCookies != null && !setCookies.isEmpty(): "No Set-Cookie headers in login response"

        def sessionCookies = combineCookies(setCookies)

        // Get fresh CSRF token with session cookies
        def csrfHeaders = new HttpHeaders()
        csrfHeaders.set(HttpHeaders.COOKIE, sessionCookies)
        def csrfRequest = new HttpEntity<>(csrfHeaders)
        def csrfResponse = restTemplate.exchange(
            "${baseUrl()}/api/auth/csrf",
            HttpMethod.GET,
            csrfRequest,
            Map
        )

        // Extract new CSRF token and cookies (including updated XSRF-TOKEN cookie)
        def csrfToken = csrfResponse.body.token
        def allCookies = combineCookies(csrfResponse.headers.get(HttpHeaders.SET_COOKIE) ?: setCookies)

        def headers = new HttpHeaders()
        headers.set(HttpHeaders.COOKIE, allCookies)
        headers.set("X-XSRF-TOKEN", csrfToken)

        def logoutRequest = new HttpEntity<>(headers)
        return restTemplate.exchange(
            "${baseUrl()}/api/auth/logout",
            HttpMethod.POST,
            logoutRequest,
            Map
        )
    }

    // ============================================================================
    // Manual Registration Tests
    // ============================================================================

    def "should register user with email and password"() {
        given: "a valid registration request"
        def request = new RegisterRequest(
            "test@example.com",
            "SecurePassword123",
            "Test User"
        )

        when: "registering a new user"
        def response = restTemplate.postForEntity(
            "${baseUrl()}/api/auth/register",
            request,
            Map
        )

        then: "user is created successfully"
        response.statusCode == HttpStatus.OK
        response.body.email == "test@example.com"
        response.body.name == "Test User"
        response.body.emailVerified == false
        response.body.hasPassword == true
        response.body.providers == []
    }

    def "should reject duplicate email"() {
        given: "an existing user"
        def request = new RegisterRequest(
            "duplicate@example.com",
            "SecurePassword123",
            "User One"
        )
        restTemplate.postForEntity(
            "${baseUrl()}/api/auth/register",
            request,
            Map
        )

        and: "a duplicate registration attempt"
        def duplicate = new RegisterRequest(
            "duplicate@example.com",
            "DifferentPassword456",
            "User Two"
        )

        when: "trying to register with same email"
        def response = restTemplate.postForEntity(
            "${baseUrl()}/api/auth/register",
            duplicate,
            Map
        )

        then: "registration is rejected"
        response.statusCode == HttpStatus.BAD_REQUEST
        response.body.error.contains("already registered")
    }

    def "should reject invalid email"() {
        given: "an invalid email address"
        def request = new RegisterRequest(
            "not-an-email",
            "SecurePassword123",
            "Test User"
        )

        when: "trying to register"
        def response = restTemplate.postForEntity(
            "${baseUrl()}/api/auth/register",
            request,
            Map
        )

        then: "registration is rejected"
        response.statusCode == HttpStatus.BAD_REQUEST
    }

    def "should reject weak password"() {
        given: "a weak password (too short and no digit)"
        def request = new RegisterRequest(
            "test@example.com",
            "short",
            "Test User"
        )

        when: "trying to register"
        def response = restTemplate.postForEntity(
            "${baseUrl()}/api/auth/register",
            request,
            Map
        )

        then: "registration is rejected with appropriate message"
        response.statusCode == HttpStatus.BAD_REQUEST
        response.body.error.contains("8-100 characters") || response.body.error.contains("letter and one digit")
    }

    // ============================================================================
    // Login Tests
    // ============================================================================

    def "should login with email and password"() {
        given: "a registered user"
        def registerRequest = new RegisterRequest(
            "login@example.com",
            "MyPassword123",
            "Login User"
        )
        restTemplate.postForEntity(
            "${baseUrl()}/api/auth/register",
            registerRequest,
            Map
        )

        when: "logging in with correct credentials"
        def response = performLogin("login@example.com", "MyPassword123")

        then: "login is successful and session is created"
        response.statusCode == HttpStatus.OK
        response.body.success == true
        response.headers.getFirst(HttpHeaders.SET_COOKIE)?.contains("JSESSIONID")
    }

    def "should reject invalid password"() {
        given: "a registered user"
        def registerRequest = new RegisterRequest(
            "wrong@example.com",
            "CorrectPassword123",
            "User"
        )
        restTemplate.postForEntity(
            "${baseUrl()}/api/auth/register",
            registerRequest,
            Map
        )

        when: "trying to login with wrong password"
        def response = performLogin("wrong@example.com", "WrongPassword")

        then: "login is rejected"
        response.statusCode == HttpStatus.UNAUTHORIZED
        response.body.error == "Invalid credentials"
    }

    def "should reject login for nonexistent user"() {
        when: "trying to login as nonexistent user"
        def response = performLogin("nonexistent@example.com", "AnyPassword123")

        then: "login is rejected"
        response.statusCode == HttpStatus.UNAUTHORIZED
    }

    // ============================================================================
    // /api/me Endpoint Tests
    // ============================================================================

    def "should return user info when authenticated"() {
        given: "a registered and logged in user"
        def registerRequest = new RegisterRequest(
            "me@example.com",
            "MyPassword123",
            "Me User"
        )
        restTemplate.postForEntity(
            "${baseUrl()}/api/auth/register",
            registerRequest,
            Map
        )

        // Login to get session cookie
        def loginResponse = performLogin("me@example.com", "MyPassword123")
        def cookies = combineCookies(loginResponse.headers.get(HttpHeaders.SET_COOKIE))

        when: "calling /api/me as authenticated user"
        def meHeaders = new HttpHeaders()
        meHeaders.set(HttpHeaders.COOKIE, cookies)

        def meRequest = new HttpEntity<>(meHeaders)
        def response = restTemplate.exchange(
            "${baseUrl()}/api/me",
            HttpMethod.GET,
            meRequest,
            Map
        )

        then: "user info is returned"
        response.statusCode == HttpStatus.OK
        response.body.email == "me@example.com"
        response.body.name == "Me User"
        response.body.hasPassword == true
    }

    def "should return 401 when not authenticated"() {
        when: "calling /api/me without authentication"
        def response = restTemplate.getForEntity(
            "${baseUrl()}/api/me",
            Map
        )

        then: "request is rejected with 401"
        response.statusCode == HttpStatus.UNAUTHORIZED
    }

    // ============================================================================
    // Session & CSRF Tests
    // ============================================================================

    def "should create session with HttpOnly cookie"() {
        given: "a registered user"
        def registerRequest = new RegisterRequest(
            "session@example.com",
            "Password123",
            "Session User"
        )
        restTemplate.postForEntity(
            "${baseUrl()}/api/auth/register",
            registerRequest,
            Map
        )

        when: "logging in"
        def response = performLogin("session@example.com", "Password123")

        then: "session cookie is HttpOnly"
        response.statusCode == HttpStatus.OK
        def setCookieHeader = response.headers.getFirst(HttpHeaders.SET_COOKIE)
        setCookieHeader?.contains("JSESSIONID")
        setCookieHeader?.contains("HttpOnly")
    }

    def "should validate CSRF token"() {
        given: "a registered user"
        def registerRequest = new RegisterRequest(
            "csrf@example.com",
            "Password123",
            "CSRF User"
        )
        restTemplate.postForEntity(
            "${baseUrl()}/api/auth/register",
            registerRequest,
            Map
        )

        when: "trying to login without CSRF token"
        def loginForm = new LinkedMultiValueMap<String, String>()
        loginForm.add("username", "csrf@example.com")
        loginForm.add("password", "Password123")

        def headers = new HttpHeaders()
        headers.contentType = MediaType.APPLICATION_FORM_URLENCODED

        def loginRequest = new HttpEntity<MultiValueMap<String, String>>(loginForm, headers)
        def responseWithoutCsrf = restTemplate.exchange(
            "${baseUrl()}/api/auth/login",
            HttpMethod.POST,
            loginRequest,
            Map
        )

        then: "request is forbidden"
        responseWithoutCsrf.statusCode == HttpStatus.FORBIDDEN

        when: "logging in with CSRF token"
        def responseWithCsrf = performLogin("csrf@example.com", "Password123")

        then: "login is successful"
        responseWithCsrf.statusCode == HttpStatus.OK
    }

    def "should logout and invalidate session"() {
        given: "a registered and logged in user"
        def registerRequest = new RegisterRequest(
            "logout@example.com",
            "Password123",
            "Logout User"
        )
        restTemplate.postForEntity(
            "${baseUrl()}/api/auth/register",
            registerRequest,
            Map
        )

        // Login to get session
        def loginResponse = performLogin("logout@example.com", "Password123")

        when: "logging out"
        def response = performLogout(loginResponse)

        then: "logout is successful"
        response.statusCode == HttpStatus.OK
        response.body.success == true
    }

    // ============================================================================
    // Account Linking Tests - Set Password
    // ============================================================================

    def "should allow authenticated user to set password"() {
        given: "a registered and logged in user"
        def registerRequest = new RegisterRequest(
            "oauth-only@example.com",
            "InitialPassword123",
            "OAuth User"
        )
        restTemplate.postForEntity(
            "${baseUrl()}/api/auth/register",
            registerRequest,
            Map
        )

        // Login to get session
        def loginResponse = performLogin("oauth-only@example.com", "InitialPassword123")
        def sessionCookies = combineCookies(loginResponse.headers.get(HttpHeaders.SET_COOKIE))

        when: "setting new password as authenticated user"
        def passwordRequest = new SetPasswordRequest("NewPassword123")

        // Get fresh CSRF token with session cookies
        def csrfHeaders = new HttpHeaders()
        csrfHeaders.set(HttpHeaders.COOKIE, sessionCookies)
        def csrfRequest = new HttpEntity<>(csrfHeaders)
        def csrfResponse = restTemplate.exchange(
            "${baseUrl()}/api/auth/csrf",
            HttpMethod.GET,
            csrfRequest,
            Map
        )

        def csrfToken = csrfResponse.body.token
        // Merge cookies: JSESSIONID from login + XSRF-TOKEN from csrf response
        def csrfCookies = csrfResponse.headers.get(HttpHeaders.SET_COOKIE)
        def allCookies = csrfCookies ? combineCookies(
            csrfCookies + loginResponse.headers.get(HttpHeaders.SET_COOKIE)) : sessionCookies

        def headers = new HttpHeaders()
        headers.set(HttpHeaders.COOKIE, allCookies)
        headers.set("X-XSRF-TOKEN", csrfToken)
        headers.contentType = MediaType.APPLICATION_JSON

        def setPasswordRequest = new HttpEntity<>(passwordRequest, headers)
        def response = restTemplate.exchange(
            "${baseUrl()}/api/auth/set-password",
            HttpMethod.POST,
            setPasswordRequest,
            Map
        )

        then: "password is set successfully"
        response.statusCode == HttpStatus.OK
        response.body.success == true
    }

    def "should require authentication for set password"() {
        given: "a password request"
        def request = new SetPasswordRequest("NewPassword123")
        def csrf = getCsrfToken()

        when: "trying to set password without authentication"
        def headers = new HttpHeaders()
        if (csrf.headerName && csrf.token) {
            headers.set(csrf.headerName, csrf.token)
        }
        if (csrf.cookie) {
            headers.set(HttpHeaders.COOKIE, csrf.cookie)
        }
        headers.contentType = MediaType.APPLICATION_JSON

        def setPasswordRequest = new HttpEntity<>(request, headers)
        def response = restTemplate.exchange(
            "${baseUrl()}/api/auth/set-password",
            HttpMethod.POST,
            setPasswordRequest,
            Map
        )

        then: "request is rejected"
        response.statusCode == HttpStatus.UNAUTHORIZED
    }
}
