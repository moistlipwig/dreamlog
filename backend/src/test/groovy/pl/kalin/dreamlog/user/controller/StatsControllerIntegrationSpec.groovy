package pl.kalin.dreamlog.user.controller

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
 * Integration tests for Stats Controller.
 * Verifies user statistics aggregation from dreams.
 */
class StatsControllerIntegrationSpec extends IntegrationSpec {

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

    def "should return empty stats when user has no dreams"() {
        given: "a user with no dreams"
        def client = new StatsClient(restTemplate, baseUrl())
        client.registerAndLogin("user@example.com", "Password123", "User")

        when: "fetching stats"
        def response = client.getMyStats()

        then: "stats show zero dreams and null mood"
        response.statusCode == HttpStatus.OK
        response.body.totalDreams == 0
        response.body.mostCommonMood == null
    }

    def "should calculate total dreams correctly"() {
        given: "a user with 5 dreams"
        def client = new StatsClient(restTemplate, baseUrl())
        client.registerAndLogin("user@example.com", "Password123", "User")

        client.createDream([
            date     : LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
            title    : "Dream 1",
            content  : "Content",
            vividness: 5,
            lucid    : false,
            tags     : []
        ])
        client.createDream([
            date     : LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
            title    : "Dream 2",
            content  : "Content",
            vividness: 5,
            lucid    : false,
            tags     : []
        ])
        client.createDream([
            date     : LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
            title    : "Dream 3",
            content  : "Content",
            vividness: 5,
            lucid    : false,
            tags     : []
        ])
        client.createDream([
            date     : LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
            title    : "Dream 4",
            content  : "Content",
            vividness: 5,
            lucid    : false,
            tags     : []
        ])
        client.createDream([
            date     : LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
            title    : "Dream 5",
            content  : "Content",
            vividness: 5,
            lucid    : false,
            tags     : []
        ])

        when: "fetching stats"
        def response = client.getMyStats()

        then: "total dreams is 5"
        response.statusCode == HttpStatus.OK
        response.body.totalDreams == 5
    }

    def "should calculate most common mood correctly"() {
        given: "a user with dreams of different moods"
        def client = new StatsClient(restTemplate, baseUrl())
        client.registerAndLogin("user@example.com", "Password123", "User")

        // 3 POSITIVE, 2 NEUTRAL, 1 NEGATIVE
        client.createDream([
            date          : LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
            title         : "Happy Dream 1",
            content       : "Content",
            moodAfterDream: "POSITIVE",
            vividness     : 5,
            lucid         : false
        ])
        client.createDream([
            date          : LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
            title         : "Happy Dream 2",
            content       : "Content",
            moodAfterDream: "POSITIVE",
            vividness     : 5,
            lucid         : false
        ])
        client.createDream([
            date          : LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
            title         : "Happy Dream 3",
            content       : "Content",
            moodAfterDream: "POSITIVE",
            vividness     : 5,
            lucid         : false
        ])
        client.createDream([
            date          : LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
            title         : "Neutral Dream 1",
            content       : "Content",
            moodAfterDream: "NEUTRAL",
            vividness     : 5,
            lucid         : false
        ])
        client.createDream([
            date          : LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
            title         : "Neutral Dream 2",
            content       : "Content",
            moodAfterDream: "NEUTRAL",
            vividness     : 5,
            lucid         : false
        ])
        client.createDream([
            date          : LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
            title         : "Sad Dream",
            content       : "Content",
            moodAfterDream: "NEGATIVE",
            vividness     : 5,
            lucid         : false
        ])

        when: "fetching stats"
        def response = client.getMyStats()

        then: "most common mood is POSITIVE"
        response.statusCode == HttpStatus.OK
        response.body.mostCommonMood == "POSITIVE"
    }

    def "should only count authenticated user's dreams"() {
        given: "two users with different amounts of dreams"
        def user1 = new StatsClient(restTemplate, baseUrl())
        user1.registerAndLogin("user1@example.com", "Password123", "User One")
        user1.createDream([
            date     : LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
            title    : "User1 Dream 1",
            content  : "Content",
            vividness: 5,
            lucid    : false,
            tags     : []
        ])
        user1.createDream([
            date     : LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
            title    : "User1 Dream 2",
            content  : "Content",
            vividness: 5,
            lucid    : false,
            tags     : []
        ])

        def user2 = new StatsClient(restTemplate, baseUrl())
        user2.registerAndLogin("user2@example.com", "Password123", "User Two")
        user2.createDream([
            date     : LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
            title    : "User2 Dream 1",
            content  : "Content",
            vividness: 5,
            lucid    : false,
            tags     : []
        ])

        when: "user1 fetches their stats"
        def user1Stats = user1.getMyStats()

        then: "user1 sees only their 2 dreams"
        user1Stats.statusCode == HttpStatus.OK
        user1Stats.body.totalDreams == 2

        when: "user2 fetches their stats"
        def user2Stats = user2.getMyStats()

        then: "user2 sees only their 1 dream"
        user2Stats.statusCode == HttpStatus.OK
        user2Stats.body.totalDreams == 1
    }

    def "should reject unauthenticated requests"() {
        given: "an unauthenticated client"
        def client = new StatsClient(restTemplate, baseUrl())

        when: "trying to fetch stats without login"
        def response = client.getMyStats()

        then: "request is rejected"
        response.statusCode == HttpStatus.UNAUTHORIZED
    }

    // ============================================================================
    // Helper Class - StatsClient
    // ============================================================================

    /**
     * Client for stats operations with cookie and CSRF management.
     */
    static class StatsClient extends SessionRestClient {

        StatsClient(TestRestTemplate rest, String baseUrl) {
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

        ResponseEntity<Map> getMyStats() {
            return get("/api/stats/me", Map)
        }

        ResponseEntity<Map> createDream(Map dream) {
            return json(HttpMethod.POST, "/api/dreams", dream, Map)
        }
    }
}
