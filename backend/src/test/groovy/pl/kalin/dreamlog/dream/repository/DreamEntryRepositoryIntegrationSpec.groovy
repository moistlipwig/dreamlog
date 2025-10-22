package pl.kalin.dreamlog.dream.repository

import jakarta.transaction.Transactional
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.jdbc.core.JdbcTemplate
import pl.kalin.dreamlog.IntegrationSpec
import pl.kalin.dreamlog.dream.model.DreamEntry
import pl.kalin.dreamlog.user.User
import pl.kalin.dreamlog.user.UserRepository

import java.time.LocalDate

@Transactional
class DreamEntryRepositoryIntegrationSpec extends IntegrationSpec {

    @Autowired
    DreamEntryRepository dreamEntryRepository

    @Autowired
    UserRepository userRepository

    @Autowired
    JdbcTemplate jdbcTemplate

    def "should perform CRUD operations"() {
        given: "a user and a dream entry"
        def user = userRepository.save(User.builder()
            .email("john@example.com")
            .name("John Doe")
            .build())

        when: "creating a dream"
        def dream = dreamEntryRepository.save(DreamEntry.builder()
            .user(user)
            .date(LocalDate.now())
            .title("First")
            .content("It was vivid")
            .build())

        then: "dream can be retrieved"
        dreamEntryRepository.findById(dream.id).isPresent()
        dreamEntryRepository.findById(dream.id).get() == dream

        when: "updating the dream"
        dream.title = "Updated"
        dreamEntryRepository.save(dream)

        then: "changes are persisted"
        with(dreamEntryRepository.findById(dream.id).get()) {
            title == "Updated"
        }

        when: "deleting the dream"
        dreamEntryRepository.deleteById(dream.id)

        then: "dream is removed"
        dreamEntryRepository.findById(dream.id).isEmpty()
    }

    def "should enforce NOT NULL constraint on title from migration"() {
        given: "a user"
        def user = userRepository.save(User.builder()
            .email("kate@example.com")
            .name("Kate Smith")
            .build())

        when: "trying to insert dream with null title"
        jdbcTemplate.update("INSERT INTO dream_entry(id, user_id, date, title, content) VALUES (?,?,?,?,?)",
            UUID.randomUUID(), user.id, LocalDate.now(), null, "content")

        then: "database constraint is violated"
        thrown(DataIntegrityViolationException)
    }
}
