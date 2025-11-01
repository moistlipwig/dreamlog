package pl.kalin.dreamlog.dream.controller

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.util.LinkedMultiValueMap
import pl.kalin.dreamlog.IntegrationSpec
import pl.kalin.dreamlog.dream.repository.DreamEntryRepository
import pl.kalin.dreamlog.support.SessionRestClient
import pl.kalin.dreamlog.user.UserRepository
import pl.kalin.dreamlog.user.dto.RegisterRequest

import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Integration tests for Dream Entry Controller.
 * Verifies user isolation: users can only access their own dreams.
 */
class DreamControllerIntegrationSpec extends IntegrationSpec {

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
            date       : LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
            title      : "My First Dream",
            content    : "I was flying over mountains",
            moodInDream: "POSITIVE",
            vividness  : 8,
            lucid      : false,
            tags       : ["flying", "nature"]
        ]
        def response = client.createDream(dream)

        then: "dream is created successfully"
        response.statusCode == HttpStatus.CREATED
        response.body.id != null // CQRS: Only ID returned
        response.headers.getLocation() != null // Location header present

        and: "fetch created dream to verify data"
        def created = client.getDreamById(response.body.id)
        created.statusCode == HttpStatus.OK
        created.body.title == "My First Dream"
        created.body.content == "I was flying over mountains"
        created.body.moodInDream == "POSITIVE"
        created.body.vividness == 8
        created.body.lucid == false
        created.body.tags == ["flying", "nature"]
    }

    def "should reject creating dream without authentication"() {
        given: "an unauthenticated client"
        def client = new DreamClient(restTemplate, baseUrl())

        when: "trying to create dream without login"
        def dream = [
            date   : LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
            title  : "Unauthorized Dream",
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
            date     : LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
            title    : "User1 Dream 1",
            content  : "User 1 content 1",
            vividness: 5,
            lucid    : false,
            tags     : []
        ])
        user1.createDream([
            date     : LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
            title    : "User1 Dream 2",
            content  : "User 1 content 2",
            vividness: 5,
            lucid    : false,
            tags     : []
        ])

        def user2 = new DreamClient(restTemplate, baseUrl())
        user2.registerAndLogin("user2@example.com", "Password123", "User Two")
        user2.createDream([
            date     : LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
            title    : "User2 Dream 1",
            content  : "User 2 content 1",
            vividness: 5,
            lucid    : false,
            tags     : []
        ])

        when: "user1 fetches their dreams"
        def user1Dreams = user1.getDreams()

        then: "user1 sees only their own 2 dreams"
        user1Dreams.statusCode == HttpStatus.OK
        user1Dreams.body.content.size() == 2
        user1Dreams.body.content.every { it.title.startsWith("User1") }

        when: "user2 fetches their dreams"
        def user2Dreams = user2.getDreams()

        then: "user2 sees only their own 1 dream"
        user2Dreams.statusCode == HttpStatus.OK
        user2Dreams.body.content.size() == 1
        user2Dreams.body.content[0].title == "User2 Dream 1"
    }

    // ============================================================================
    // Pagination Tests
    // ============================================================================

    def "should paginate dreams with default parameters"() {
        given: "a user with 5 dreams"
        def client = new DreamClient(restTemplate, baseUrl())
        client.registerAndLogin("user@example.com", "Password123", "User")

        // Create dreams one by one to avoid closure issues
        def dream1 = [date                          : LocalDate.now().minusDays(1).format(
            DateTimeFormatter.ISO_LOCAL_DATE), title: "Dream 1", content: "Content 1", vividness: 5, lucid: false, tags: []]
        def dream2 = [date                          : LocalDate.now().minusDays(2).format(
            DateTimeFormatter.ISO_LOCAL_DATE), title: "Dream 2", content: "Content 2", vividness: 5, lucid: false, tags: []]
        def dream3 = [date                          : LocalDate.now().minusDays(3).format(
            DateTimeFormatter.ISO_LOCAL_DATE), title: "Dream 3", content: "Content 3", vividness: 5, lucid: false, tags: []]
        def dream4 = [date                          : LocalDate.now().minusDays(4).format(
            DateTimeFormatter.ISO_LOCAL_DATE), title: "Dream 4", content: "Content 4", vividness: 5, lucid: false, tags: []]
        def dream5 = [date                          : LocalDate.now().minusDays(5).format(
            DateTimeFormatter.ISO_LOCAL_DATE), title: "Dream 5", content: "Content 5", vividness: 5, lucid: false, tags: []]

        client.createDream(dream1)
        client.createDream(dream2)
        client.createDream(dream3)
        client.createDream(dream4)
        client.createDream(dream5)

        when: "fetching dreams without pagination params"
        def response = client.getDreams()

        then: "returns paginated response with default size 20"
        response.statusCode == HttpStatus.OK
        response.body.content.size() == 5
        response.body.totalElements == 5
        response.body.totalPages == 1
        response.body.size == 20
        response.body.number == 0
    }


    // ============================================================================
    // Get Single Dream Tests - Authorization
    // ============================================================================

    def "should get dream by ID when user owns it"() {
        given: "a user with a dream"
        def client = new DreamClient(restTemplate, baseUrl())
        client.registerAndLogin("owner@example.com", "Password123", "Owner")
        def created = client.createDream([
            date     : LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
            title    : "My Dream",
            content  : "My content",
            vividness: 5,
            lucid    : false,
            tags     : []
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
            date     : LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
            title    : "User1 Dream",
            content  : "Private content",
            vividness: 5,
            lucid    : false,
            tags     : []
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
            date     : LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
            title    : "Original Title",
            content  : "Original Content",
            vividness: 5,
            lucid    : false,
            tags     : []
        ])
        def dreamId = created.body.id

        when: "updating the dream"
        def updated = [
            date     : LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
            title    : "Updated Title",
            content  : "Updated Content",
            vividness: 9
        ]
        def response = client.updateDream(dreamId, updated)

        then: "dream is updated successfully"
        response.statusCode == HttpStatus.NO_CONTENT

        and: "fetch updated dream to verify changes"
        def fetched = client.getDreamById(dreamId)
        fetched.statusCode == HttpStatus.OK
        fetched.body.id == dreamId
        fetched.body.title == "Updated Title"
        fetched.body.content == "Updated Content"
        fetched.body.vividness == 9
    }

    def "should deny updating dream belonging to other user"() {
        given: "user1 creates a dream"
        def user1 = new DreamClient(restTemplate, baseUrl())
        user1.registerAndLogin("user1@example.com", "Password123", "User One")
        def created = user1.createDream([
            date     : LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
            title    : "User1 Dream",
            content  : "Original",
            vividness: 5,
            lucid    : false,
            tags     : []
        ])
        def dreamId = created.body.id

        and: "user2 is logged in"
        def user2 = new DreamClient(restTemplate, baseUrl())
        user2.registerAndLogin("user2@example.com", "Password123", "User Two")

        when: "user2 tries to update user1's dream"
        def maliciousUpdate = [
            date   : LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
            title  : "Hacked Title",
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
            date     : LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
            title    : "Dream to Delete",
            content  : "Will be deleted",
            vividness: 5,
            lucid    : false,
            tags     : []
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
            date     : LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
            title    : "User1 Important Dream",
            content  : "Should not be deleted",
            vividness: 5,
            lucid    : false,
            tags     : []
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
    static class DreamClient extends SessionRestClient {

        DreamClient(TestRestTemplate rest, String baseUrl) {
            super(rest, baseUrl)
        }

        void registerAndLogin(String email, String password, String name) {
            register(email, password, name)
            login(email, password)
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

        // Dream-specific methods

        ResponseEntity<Map> createDream(Map dream) {
            return json(HttpMethod.POST, "/api/dreams", dream, Map)
        }

        ResponseEntity<Map> getDreams(int page = 0, int size = 20, String sort = "date,desc") {
            def url = "/api/dreams?page=${page}&size=${size}&sort=${sort}"
            return get(url, Map)
        }

        ResponseEntity<Map> getDreamById(String dreamId) {
            return get("/api/dreams/${dreamId}", Map)
        }

        ResponseEntity<Map> updateDream(String dreamId, Map dream) {
            return json(HttpMethod.PUT, "/api/dreams/${dreamId}", dream, Map)
        }

        ResponseEntity<Void> deleteDream(String dreamId) {
            return delete("/api/dreams/${dreamId}", Void)
        }
    }
}
