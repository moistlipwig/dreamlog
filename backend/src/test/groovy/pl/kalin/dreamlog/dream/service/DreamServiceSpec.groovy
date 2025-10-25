package pl.kalin.dreamlog.dream.service

import org.springframework.security.access.AccessDeniedException
import pl.kalin.dreamlog.dream.dto.DreamCreateRequest
import pl.kalin.dreamlog.dream.dto.DreamUpdateRequest
import pl.kalin.dreamlog.dream.model.DreamEntry
import pl.kalin.dreamlog.dream.model.Mood
import pl.kalin.dreamlog.dream.repository.DreamEntryRepository
import pl.kalin.dreamlog.user.User
import spock.lang.Specification

import java.time.LocalDate

/**
 * Unit test for DreamService.
 * Tests business logic and authorization without Spring context.
 */
class DreamServiceSpec extends Specification {

    DreamEntryRepository dreamRepository = Mock()
    DreamService dreamService = new DreamService(dreamRepository)

    User testUser = User.builder()
        .id(UUID.randomUUID())
        .email("test@example.com")
        .name("Test User")
        .build()

    User otherUser = User.builder()
        .id(UUID.randomUUID())
        .email("other@example.com")
        .name("Other User")
        .build()

    def "getUserDreams should return all dreams for the user"() {
        given: "User has two dreams in database"
        def dream1 = createDreamEntity(testUser, "Dream 1")
        def dream2 = createDreamEntity(testUser, "Dream 2")

        when: "Getting user dreams"
        def result = dreamService.getUserDreams(testUser)

        then: "Repository is called with user ID"
        1 * dreamRepository.findByUserId(testUser.id) >> [dream1, dream2]

        and: "Returns list of DreamResponse DTOs"
        result.size() == 2
        result[0].title() == "Dream 1"
        result[1].title() == "Dream 2"
    }

    def "getUserDreams should return empty list when user has no dreams"() {
        when: "Getting dreams for user with no dreams"
        def result = dreamService.getUserDreams(testUser)

        then: "Repository returns empty list"
        1 * dreamRepository.findByUserId(testUser.id) >> []

        and: "Returns empty list"
        result.isEmpty()
    }

    def "getDreamById should return dream when it belongs to user"() {
        given: "User has a dream"
        def dreamId = UUID.randomUUID()
        def dream = createDreamEntity(testUser, "My Dream", dreamId)

        when: "Getting dream by ID"
        def result = dreamService.getDreamById(testUser, dreamId)

        then: "Repository is called with dream ID and user ID"
        1 * dreamRepository.findByIdAndUserId(dreamId, testUser.id) >> Optional.of(dream)

        and: "Returns DreamResponse"
        result.id() == dreamId
        result.title() == "My Dream"
    }

    def "getDreamById should throw AccessDeniedException when dream belongs to other user"() {
        given: "Dream ID"
        def dreamId = UUID.randomUUID()

        when: "Trying to get dream that doesn't belong to user"
        dreamService.getDreamById(testUser, dreamId)

        then: "Repository returns empty (dream not found for this user)"
        1 * dreamRepository.findByIdAndUserId(dreamId, testUser.id) >> Optional.empty()

        and: "AccessDeniedException is thrown"
        thrown(AccessDeniedException)
    }

    def "createDream should save dream with user as owner"() {
        given: "Create request"
        def request = new DreamCreateRequest(
            LocalDate.of(2025, 10, 22),
            "Test Dream",
            "Dream content",
            Mood.POSITIVE,
            Mood.POSITIVE,
            8,
            true,
            ["tag1", "tag2"]
        )

        and: "Saved dream entity"
        def savedDream = createDreamEntity(testUser, request.title())

        when: "Creating dream"
        def result = dreamService.createDream(testUser, request)

        then: "Repository saves dream with correct data"
        1 * dreamRepository.save({ DreamEntry dream ->
            dream.user.id == testUser.id &&
                dream.title == request.title() &&
                dream.content == request.content() &&
                dream.date == request.date() &&
                dream.moodInDream == request.moodInDream() &&
                dream.moodAfterDream == request.moodAfterDream() &&
                dream.vividness == request.vividness() &&
                dream.lucid == request.lucid() &&
                dream.tags == request.tags()
        }) >> savedDream

        and: "Returns DreamResponse"
        result.title() == request.title()
    }

    def "createDream should set default values for optional fields"() {
        given: "Create request with nulls for optional fields"
        def request = new DreamCreateRequest(
            LocalDate.now(),
            "Dream",
            "Content",
            null,  // moodInDream
            null,  // moodAfterDream
            null,  // vividness
            null,  // lucid
            null   // tags
        )

        and: "Saved dream"
        def savedDream = createDreamEntity(testUser, request.title())

        when: "Creating dream"
        dreamService.createDream(testUser, request)

        then: "Repository saves dream with defaults"
        1 * dreamRepository.save({ DreamEntry dream ->
            dream.vividness == 0 &&
                !dream.lucid &&
                dream.tags == []
        }) >> savedDream
    }

    def "updateDream should update dream when it belongs to user"() {
        given: "Existing dream"
        def dreamId = UUID.randomUUID()
        def existingDream = createDreamEntity(testUser, "Old Title", dreamId)

        and: "Update request"
        def request = new DreamUpdateRequest(
            LocalDate.now(),
            "New Title",
            "New Content",
            Mood.NEGATIVE,
            Mood.NEUTRAL,
            5,
            false,
            ["new-tag"]
        )

        when: "Updating dream"
        def result = dreamService.updateDream(testUser, dreamId, request)

        then: "Repository finds dream by ID and user ID"
        1 * dreamRepository.findByIdAndUserId(dreamId, testUser.id) >> Optional.of(existingDream)

        and: "Dream is updated"
        existingDream.title == request.title()
        existingDream.content == request.content()
        existingDream.date == request.date()
        existingDream.moodInDream == request.moodInDream()
        existingDream.moodAfterDream == request.moodAfterDream()
        existingDream.vividness == request.vividness()
        existingDream.lucid == request.lucid()
        existingDream.tags == request.tags()

        and: "Repository saves updated dream"
        1 * dreamRepository.save(existingDream) >> existingDream

        and: "Returns updated DreamResponse"
        result.title() == request.title()
    }

    def "updateDream should throw AccessDeniedException when dream belongs to other user"() {
        given: "Dream ID and update request"
        def dreamId = UUID.randomUUID()
        def request = new DreamUpdateRequest(
            LocalDate.now(),
            "Title",
            "Content",
            null, null, null, null, null
        )

        when: "Trying to update dream that doesn't belong to user"
        dreamService.updateDream(testUser, dreamId, request)

        then: "Repository returns empty"
        1 * dreamRepository.findByIdAndUserId(dreamId, testUser.id) >> Optional.empty()

        and: "AccessDeniedException is thrown"
        thrown(AccessDeniedException)

        and: "Repository save is never called"
        0 * dreamRepository.save(_)
    }

    def "deleteDream should delete dream when it belongs to user"() {
        given: "Existing dream"
        def dreamId = UUID.randomUUID()
        def existingDream = createDreamEntity(testUser, "Dream to delete", dreamId)

        when: "Deleting dream"
        dreamService.deleteDream(testUser, dreamId)

        then: "Repository finds dream by ID and user ID"
        1 * dreamRepository.findByIdAndUserId(dreamId, testUser.id) >> Optional.of(existingDream)

        and: "Repository deletes the dream"
        1 * dreamRepository.delete(existingDream)
    }

    def "deleteDream should throw AccessDeniedException when dream belongs to other user"() {
        given: "Dream ID"
        def dreamId = UUID.randomUUID()

        when: "Trying to delete dream that doesn't belong to user"
        dreamService.deleteDream(testUser, dreamId)

        then: "Repository returns empty"
        1 * dreamRepository.findByIdAndUserId(dreamId, testUser.id) >> Optional.empty()

        and: "AccessDeniedException is thrown"
        thrown(AccessDeniedException)

        and: "Repository delete is never called"
        0 * dreamRepository.delete(_)
    }

    // ============================================================================
    // Helper methods
    // ============================================================================

    private static DreamEntry createDreamEntity(User user, String title, UUID id = UUID.randomUUID()) {
        return DreamEntry.builder()
            .id(id)
            .user(user)
            .date(LocalDate.now())
            .title(title)
            .content("Dream content for: " + title)
            .moodInDream(Mood.NEUTRAL)
            .moodAfterDream(Mood.NEUTRAL)
            .vividness(5)
            .lucid(false)
            .tags([])
            .build()
    }
}
