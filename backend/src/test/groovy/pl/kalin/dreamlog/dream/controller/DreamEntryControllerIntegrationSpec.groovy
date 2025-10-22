package pl.kalin.dreamlog.dream.controller

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
import pl.kalin.dreamlog.dream.repository.DreamEntryRepository
import pl.kalin.dreamlog.user.UserRepository
import pl.kalin.dreamlog.user.dto.RegisterRequest

import java.time.LocalDate

/**
 * Integration tests for Dream Entry Controller.
 * Verifies user isolation: users can only access their own dreams.
 */
class DreamEntryControllerIntegrationSpec extends IntegrationSpec {

    @LocalServerPort
    int port

    @Autowired
    TestRestTemplate restTemplate

    @Autowired
    UserRepository userRepository

    @Autowired
    DreamEntryRepository dreamRepository

    String baseUrl() {
        "http://localhost:${port}"
    }

    def setup() {
        dreamRepository.deleteAll()
        userRepository.deleteAll()
    }

    // ============================================================================
    // Create Dream Tests
    // ============================================================================

    def "should create dream for authenticated user"() {
        given: "a logged in user"
        def client = new DreamClient(restTemplate, baseUrl())
        client.registerAndLogin("user1@example.com", "Password123", "User One")

        when: "creating a dream"
        def dream = [
            date: LocalDate.now().toString(),
            title: "My First Dream",
            content: "I was flying over mountains",
            moodInDream: "HAPPY",
            vividness: 8,
            lucid: false,
            tags: ["flying", "nature"]
        ]
        def response = client.createDream(dream)

        then: "dream is created successfully"
        response.statusCode == HttpStatus.CREATED
        response.body.title == "My First Dream"
        response.body.content == "I was flying over mountains"
        response.body.moodInDream == "HAPPY"
        response.body.vividness == 8
        response.body.lucid == false
        response.body.tags == ["flying", "nature"]
        response.body.id != null
    }

    def "should reject creating dream without authentication"() {
        given: "an unauthenticated client"
        def client = new DreamClient(restTemplate, baseUrl())

        when: "trying to create dream without login"
        def dream = [
            date: LocalDate.now().toString(),
            title: "Unauthorized Dream",
            content: "This should not be created"
        ]
        def response = client.createDream(dream)

        then: "request is rejected"
        response.statusCode == HttpStatus.UNAUTHORIZED
    }

    // ============================================================================
    // Get Dreams Tests - User Isolation
    // ============================================================================

    def "should return only user's own dreams"() {
        given: "two users with their own dreams"
        def user1 = new DreamClient(restTemplate, baseUrl())
        user1.registerAndLogin("user1@example.com", "Password123", "User One")
        user1.createDream([
            date: LocalDate.now().toString(),
            title: "User1 Dream 1",
            content: "User 1 content 1"
        ])
        user1.createDream([
            date: LocalDate.now().toString(),
            title: "User1 Dream 2",
            content: "User 1 content 2"
        ])

        def user2 = new DreamClient(restTemplate, baseUrl())
        user2.registerAndLogin("user2@example.com", "Password123", "User Two")
        user2.createDream([
            date: LocalDate.now().toString(),
            title: "User2 Dream 1",
            content: "User 2 content 1"
        ])

        when: "user1 fetches their dreams"
        def user1Dreams = user1.getDreams()

        then: "user1 sees only their own 2 dreams"
        user1Dreams.statusCode == HttpStatus.OK
        user1Dreams.body.size() == 2
        user1Dreams.body.every { it.title.startsWith("User1") }

        when: "user2 fetches their dreams"
        def user2Dreams = user2.getDreams()

        then: "user2 sees only their own 1 dream"
        user2Dreams.statusCode == HttpStatus.OK
        user2Dreams.body.size() == 1
        user2Dreams.body[0].title == "User2 Dream 1"
    }

    // ============================================================================
    // Get Single Dream Tests - Authorization
    // ============================================================================

    def "should get dream by ID when user owns it"() {
        given: "a user with a dream"
        def client = new DreamClient(restTemplate, baseUrl())
        client.registerAndLogin("owner@example.com", "Password123", "Owner")
        def created = client.createDream([
            date: LocalDate.now().toString(),
            title: "My Dream",
            content: "My content"
        ])
        def dreamId = created.body.id

        when: "fetching the dream by ID"
        def response = client.getDreamById(dreamId)

        then: "dream is returned"
        response.statusCode == HttpStatus.OK
        response.body.id == dreamId
        response.body.title == "My Dream"
    }

    def "should deny access to dream belonging to other user"() {
        given: "two users"
        def user1 = new DreamClient(restTemplate, baseUrl())
        user1.registerAndLogin("user1@example.com", "Password123", "User One")
        def dreamCreated = user1.createDream([
            date: LocalDate.now().toString(),
            title: "User1 Dream",
            content: "Private content"
        ])
        def dreamId = dreamCreated.body.id

        and: "user2 is logged in"
        def user2 = new DreamClient(restTemplate, baseUrl())
        user2.registerAndLogin("user2@example.com", "Password123", "User Two")

        when: "user2 tries to access user1's dream"
        def response = user2.getDreamById(dreamId)

        then: "access is denied"
        response.statusCode == HttpStatus.FORBIDDEN
    }

    // ============================================================================
    // Update Dream Tests - Authorization
    // ============================================================================

    def "should update dream when user owns it"() {
        given: "a user with a dream"
        def client = new DreamClient(restTemplate, baseUrl())
        client.registerAndLogin("owner@example.com", "Password123", "Owner")
        def created = client.createDream([
            date: LocalDate.now().toString(),
            title: "Original Title",
            content: "Original Content",
            vividness: 5
        ])
        def dreamId = created.body.id

        when: "updating the dream"
        def updated = [
            date: LocalDate.now().toString(),
            title: "Updated Title",
            content: "Updated Content",
            vividness: 9
        ]
        def response = client.updateDream(dreamId, updated)

        then: "dream is updated successfully"
        response.statusCode == HttpStatus.OK
        response.body.id == dreamId
        response.body.title == "Updated Title"
        response.body.content == "Updated Content"
        response.body.vividness == 9
    }

    def "should deny updating dream belonging to other user"() {
        given: "user1 creates a dream"
        def user1 = new DreamClient(restTemplate, baseUrl())
        user1.registerAndLogin("user1@example.com", "Password123", "User One")
        def created = user1.createDream([
            date: LocalDate.now().toString(),
            title: "User1 Dream",
            content: "Original"
        ])
        def dreamId = created.body.id

        and: "user2 is logged in"
        def user2 = new DreamClient(restTemplate, baseUrl())
        user2.registerAndLogin("user2@example.com", "Password123", "User Two")

        when: "user2 tries to update user1's dream"
        def maliciousUpdate = [
            date: LocalDate.now().toString(),
            title: "Hacked Title",
            content: "Hacked Content"
        ]
        def response = user2.updateDream(dreamId, maliciousUpdate)

        then: "update is denied"
        response.statusCode == HttpStatus.FORBIDDEN

        when: "user1 verifies their dream is unchanged"
        def verification = user1.getDreamById(dreamId)

        then: "original title is preserved"
        verification.body.title == "User1 Dream"
        verification.body.content == "Original"
    }

    // ============================================================================
    // Delete Dream Tests - Authorization
    // ============================================================================

    def "should delete dream when user owns it"() {
        given: "a user with a dream"
        def client = new DreamClient(restTemplate, baseUrl())
        client.registerAndLogin("owner@example.com", "Password123", "Owner")
        def created = client.createDream([
            date: LocalDate.now().toString(),
            title: "Dream to Delete",
            content: "Will be deleted"
        ])
        def dreamId = created.body.id

        when: "deleting the dream"
        def response = client.deleteDream(dreamId)

        then: "dream is deleted"
        response.statusCode == HttpStatus.NO_CONTENT

        when: "trying to fetch deleted dream"
        def fetchResponse = client.getDreamById(dreamId)

        then: "dream is not found"
        fetchResponse.statusCode == HttpStatus.FORBIDDEN
    }

    def "should deny deleting dream belonging to other user"() {
        given: "user1 creates a dream"
        def user1 = new DreamClient(restTemplate, baseUrl())
        user1.registerAndLogin("user1@example.com", "Password123", "User One")
        def created = user1.createDream([
            date: LocalDate.now().toString(),
            title: "User1 Important Dream",
            content: "Should not be deleted"
        ])
        def dreamId = created.body.id

        and: "user2 is logged in"
        def user2 = new DreamClient(restTemplate, baseUrl())
        user2.registerAndLogin("user2@example.com", "Password123", "User Two")

        when: "user2 tries to delete user1's dream"
        def response = user2.deleteDream(dreamId)

        then: "deletion is denied"
        response.statusCode == HttpStatus.FORBIDDEN

        when: "user1 verifies their dream still exists"
        def verification = user1.getDreamById(dreamId)

        then: "dream is intact"
        verification.statusCode == HttpStatus.OK
        verification.body.title == "User1 Important Dream"
    }

    // ============================================================================
    // Helper Class - DreamClient
    // ============================================================================

    /**
     * Client for dream operations with cookie and CSRF management.
     * Extends auth functionality for dream-specific endpoints.
     */
    static class DreamClient {
        private final TestRestTemplate rest
        private final String baseUrl
        private String cookies = null

        DreamClient(TestRestTemplate rest, String baseUrl) {
            this.rest = rest
            this.baseUrl = baseUrl
        }

        void registerAndLogin(String email, String password, String name) {
            register(email, password, name)
            login(email, password)
        }

        private HttpHeaders headersWithCookies() {
            def headers = new HttpHeaders()
            if (cookies) {
                headers.set(HttpHeaders.COOKIE, cookies)
            }
            return headers
        }

        private void updateCookiesFromResponse(ResponseEntity<?> response) {
            def setCookies = response.headers.get(HttpHeaders.SET_COOKIE)
            if (setCookies) {
                def cookieMap = [:]
                if (this.cookies) {
                    this.cookies.split("; ").each { cookie ->
                        def parts = cookie.split("=", 2)
                        if (parts.length == 2) {
                            cookieMap[parts[0]] = parts[1]
                        }
                    }
                }

                setCookies.each { cookieHeader ->
                    def cookiePart = cookieHeader.split(";")[0].trim()
                    def parts = cookiePart.split("=", 2)
                    if (parts.length == 2) {
                        if (parts[1].isEmpty() || cookieHeader.contains("Max-Age=0")) {
                            cookieMap.remove(parts[0])
                        } else {
                            cookieMap[parts[0]] = parts[1]
                        }
                    }
                }

                this.cookies = cookieMap.collect { k, v -> "${k}=${v}" }.join("; ")
            }
        }

        ResponseEntity<Map> register(String email, String password, String name) {
            def request = new RegisterRequest(email, password, name)
            def response = rest.postForEntity("${baseUrl}/api/auth/register", request, Map)
            updateCookiesFromResponse(response)
            return response
        }

        ResponseEntity<Map> login(String email, String password) {
            def csrf = csrf()

            def loginForm = new LinkedMultiValueMap<String, String>()
            loginForm.add("username", email)
            loginForm.add("password", password)

            def headers = headersWithCookies()
            headers.contentType = MediaType.APPLICATION_FORM_URLENCODED
            headers.set(csrf.headerName as String, csrf.token as String)

            def loginRequest = new HttpEntity<>(loginForm, headers)
            def response = rest.exchange("${baseUrl}/api/auth/login", HttpMethod.POST, loginRequest, Map)
            updateCookiesFromResponse(response)
            return response
        }

        Map csrf() {
            def headers = headersWithCookies()
            def request = new HttpEntity<>(headers)
            def response = rest.exchange("${baseUrl}/api/auth/csrf", HttpMethod.GET, request, Map)
            updateCookiesFromResponse(response)
            return [
                token     : response.body.token,
                headerName: response.body.headerName
            ]
        }

        // Dream-specific methods

        ResponseEntity<Map> createDream(Map dream) {
            def csrf = csrf()
            def headers = headersWithCookies()
            headers.contentType = MediaType.APPLICATION_JSON
            headers.set(csrf.headerName as String, csrf.token as String)

            def request = new HttpEntity<>(dream, headers)
            def response = rest.exchange("${baseUrl}/api/dreams", HttpMethod.POST, request, Map)
            updateCookiesFromResponse(response)
            return response
        }

        ResponseEntity<List> getDreams() {
            def headers = headersWithCookies()
            def request = new HttpEntity<>(headers)
            return rest.exchange("${baseUrl}/api/dreams", HttpMethod.GET, request, List)
        }

        ResponseEntity<Map> getDreamById(String dreamId) {
            def headers = headersWithCookies()
            def request = new HttpEntity<>(headers)
            return rest.exchange("${baseUrl}/api/dreams/${dreamId}", HttpMethod.GET, request, Map)
        }

        ResponseEntity<Map> updateDream(String dreamId, Map dream) {
            def csrf = csrf()
            def headers = headersWithCookies()
            headers.contentType = MediaType.APPLICATION_JSON
            headers.set(csrf.headerName as String, csrf.token as String)

            def request = new HttpEntity<>(dream, headers)
            def response = rest.exchange("${baseUrl}/api/dreams/${dreamId}", HttpMethod.PUT, request, Map)
            updateCookiesFromResponse(response)
            return response
        }

        ResponseEntity<Void> deleteDream(String dreamId) {
            def csrf = csrf()
            def headers = headersWithCookies()
            headers.set(csrf.headerName as String, csrf.token as String)

            def request = new HttpEntity<>(headers)
            return rest.exchange("${baseUrl}/api/dreams/${dreamId}", HttpMethod.DELETE, request, Void)
        }
    }
}
