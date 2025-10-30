package pl.kalin.dreamlog.dream.repository

import jakarta.transaction.Transactional
import org.springframework.beans.factory.annotation.Autowired
import pl.kalin.dreamlog.IntegrationSpec
import pl.kalin.dreamlog.dream.model.DreamEntry
import pl.kalin.dreamlog.user.User
import pl.kalin.dreamlog.user.UserRepository

import java.time.LocalDate

/**
 * Integration tests for PostgreSQL Full-Text Search functionality.
 * Tests FTS with Polish character support (unaccent) and fuzzy matching (pg_trgm).
 */
@Transactional
class DreamEntrySearchIntegrationSpec extends IntegrationSpec {

    @Autowired
    DreamEntryRepository dreamEntryRepository

    @Autowired
    UserRepository userRepository

    User testUser
    User otherUser

    def setup() {
        // Create test users
        testUser = userRepository.save(User.builder()
            .email("dreamer@example.com")
            .name("Dream Test")
            .build())

        otherUser = userRepository.save(User.builder()
            .email("other@example.com")
            .name("Other User")
            .build())

        // Create test dreams with Polish characters and various content
        dreamEntryRepository.saveAll([
            DreamEntry.builder()
                .user(testUser)
                .date(LocalDate.now())
                .title("Lucid dream about flying")
                .content("I realized I was dreaming and started to fly over the city")
                .tags(["lucid", "flying"])
                .build(),

            DreamEntry.builder()
                .user(testUser)
                .date(LocalDate.now().minusDays(1))
                .title("Nightmare with monsters")
                .content("Scary creatures were chasing me through dark corridors")
                .tags(["nightmare", "scary"])
                .build(),

            DreamEntry.builder()
                .user(testUser)
                .date(LocalDate.now().minusDays(2))
                .title("Sen o łodzi na Wiśle")
                .content("Płynąłem łodzią po Wiśle, było pięknie i spokojnie")
                .tags(["łódź", "Wisła", "Polska"])
                .build(),

            DreamEntry.builder()
                .user(testUser)
                .date(LocalDate.now().minusDays(3))
                .title("Flying car adventure")
                .content("I was driving a flying car through the mountains")
                .tags(["flying", "car"])
                .build(),

            DreamEntry.builder()
                .user(testUser)
                .date(LocalDate.now().minusDays(4))
                .title("Meeting with famous person")
                .content("I met a celebrity at a party and we had a great conversation")
                .tags(["celebrity", "party"])
                .build(),

            // Dream for other user (should not appear in testUser's search results)
            DreamEntry.builder()
                .user(otherUser)
                .date(LocalDate.now())
                .title("Lucid dream")
                .content("This is a lucid dream but belongs to another user")
                .build()
        ])
    }

    def "should search dreams by full-text query"() {
        when: "searching for 'lucid'"
        def results = dreamEntryRepository.searchByFullText(testUser.id, "lucid")

        then: "only testUser's lucid dream is returned"
        results.size() == 1
        results[0].title == "Lucid dream about flying"
    }

    def "should search dreams with multiple words"() {
        when: "searching for 'flying car'"
        def results = dreamEntryRepository.searchByFullText(testUser.id, "flying car")

        then: "dreams with both words are prioritized"
        // websearch_to_tsquery treats 'flying car' as AND by default (both words required)
        results.size() >= 1
        // "Flying car adventure" should be first (has both words)
        results[0].title == "Flying car adventure"
    }

    def "should exclude results with boolean NOT operator"() {
        when: "searching for 'flying -nightmare'"
        def results = dreamEntryRepository.searchByFullText(testUser.id, "flying -nightmare")

        then: "dreams with 'flying' but not 'nightmare' are returned"
        results.size() == 2
        results*.title.containsAll([
            "Lucid dream about flying",
            "Flying car adventure"
        ])
        !results*.title.contains("Nightmare with monsters")
    }

    def "should handle Polish characters with unaccent"() {
        when: "searching for 'lodz' (without Polish characters)"
        def results = dreamEntryRepository.searchByFullText(testUser.id, "lodz")

        then: "dream with 'łodzi' is found"
        results.size() == 1
        results[0].title == "Sen o łodzi na Wiśle"
    }

    def "should handle Polish characters in query"() {
        when: "searching for 'łódź' (with Polish characters)"
        def results = dreamEntryRepository.searchByFullText(testUser.id, "łódź")

        then: "dream with 'łodzi' is found (unaccent normalizes both)"
        results.size() == 1
        results[0].title == "Sen o łodzi na Wiśle"
    }

    def "should search in tags as well"() {
        when: "searching for tag 'Wisła'"
        def results = dreamEntryRepository.searchByFullText(testUser.id, "Wisła")

        then: "dream with matching tag is found"
        results.size() == 1
        results[0].title == "Sen o łodzi na Wiśle"
    }

    def "should limit results to 100"() {
        given: "more than 100 dreams"
        def manyDreams = (1..105).collect { i ->
            DreamEntry.builder()
                .user(testUser)
                .date(LocalDate.now())
                .title("Dream $i")
                .content("This is a common dream about something")
                .build()
        }
        dreamEntryRepository.saveAll(manyDreams)

        when: "searching for common term"
        def results = dreamEntryRepository.searchByFullText(testUser.id, "common")

        then: "results are limited to 100"
        results.size() == 100
    }

    def "should only return dreams for specified user"() {
        when: "testUser searches for 'lucid'"
        def results = dreamEntryRepository.searchByFullText(testUser.id, "lucid")

        then: "only testUser's dream is returned"
        results.size() == 1
        results[0].user.id == testUser.id
    }

    def "should return empty list when no matches"() {
        when: "searching for non-existent term"
        def results = dreamEntryRepository.searchByFullText(testUser.id, "unicorn rainbow sparkles")

        then: "no results are returned"
        results.isEmpty()
    }

    def "should handle fuzzy search for typos"() {
        when: "searching with typo 'lucdi dream' instead of 'lucid dream'"
        def results = dreamEntryRepository.searchByFuzzy(testUser.id, "lucdi dream")

        then: "similar words are found with trigram matching"
        results.size() >= 1
        results.any { it.title.toLowerCase().contains("lucid") || it.content.toLowerCase().contains("lucid") }
    }


    def "should rank fuzzy results by similarity"() {
        given: "additional dreams with varying similarity"
        dreamEntryRepository.saveAll([
            DreamEntry.builder()
                .user(testUser)
                .date(LocalDate.now())
                .title("Lucidity test")
                .content("Testing lucidity levels")
                .build(),
            DreamEntry.builder()
                .user(testUser)
                .date(LocalDate.now())
                .title("Lucy's birthday")
                .content("A party for Lucy")
                .build()
        ])

        when: "fuzzy searching for 'lucid'"
        def results = dreamEntryRepository.searchByFuzzy(testUser.id, "lucid")

        then: "results are ordered by similarity"
        results.size() >= 2
        // More similar results come first
        results[0].title.toLowerCase().contains("lucid") ||
            results[0].content.toLowerCase().contains("lucid")
    }

    def "should handle empty search query gracefully"() {
        when: "searching with empty query"
        def ftsResults = dreamEntryRepository.searchByFullText(testUser.id, "")
        def fuzzyResults = dreamEntryRepository.searchByFuzzy(testUser.id, "")

        then: "no errors and empty results"
        notThrown(Exception)
        ftsResults.isEmpty()
        fuzzyResults.isEmpty()
    }

    def "should search across title, content, and tags"() {
        when: "searching for term that appears in different fields"
        def results = dreamEntryRepository.searchByFullText(testUser.id, "flying")

        then: "all dreams with 'flying' in any field are found"
        results.size() == 2
        results*.title.containsAll([
            "Lucid dream about flying",
            "Flying car adventure"
        ])
    }

    def "should handle phrase search with quotes"() {
        when: "searching for exact phrase"
        def results = dreamEntryRepository.searchByFullText(testUser.id, '"flying car"')

        then: "only dreams with exact phrase are found"
        results.size() == 1
        results[0].title == "Flying car adventure"
    }

    def "should be case insensitive"() {
        when: "searching with different cases"
        def lowerResults = dreamEntryRepository.searchByFullText(testUser.id, "lucid")
        def upperResults = dreamEntryRepository.searchByFullText(testUser.id, "LUCID")
        def mixedResults = dreamEntryRepository.searchByFullText(testUser.id, "LuCiD")

        then: "all return same results"
        lowerResults.size() == upperResults.size()
        lowerResults.size() == mixedResults.size()
    }
}
