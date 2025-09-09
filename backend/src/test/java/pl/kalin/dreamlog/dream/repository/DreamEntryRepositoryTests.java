package pl.kalin.dreamlog.dream.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import pl.kalin.dreamlog.IntegrationTestBase;
import pl.kalin.dreamlog.dream.model.DreamEntry;
import pl.kalin.dreamlog.user.User;
import pl.kalin.dreamlog.user.UserRepository;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class DreamEntryRepositoryTests extends IntegrationTestBase {

    @Autowired
    private DreamEntryRepository dreamEntryRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldPerformCrudOperations() {
        User user = userRepository.save(User.builder().username("john").email("john@example.com").build());

        DreamEntry dream = dreamEntryRepository.save(DreamEntry.builder()
                .user(user)
                .date(LocalDate.now())
                .title("First")
                .content("It was vivid")
                .build());

        assertThat(dreamEntryRepository.findById(dream.getId())).contains(dream);

        dream.setTitle("Updated");
        dreamEntryRepository.save(dream);
        assertThat(dreamEntryRepository.findById(dream.getId())).get()
                .extracting(DreamEntry::getTitle)
                .isEqualTo("Updated");

        dreamEntryRepository.deleteById(dream.getId());
        assertThat(dreamEntryRepository.findById(dream.getId())).isEmpty();
    }

    @Test
    void shouldEnforceNotNullConstraintFromMigration() {
        User user = userRepository.save(User.builder().username("kate").email("kate@example.com").build());

        assertThatThrownBy(() -> jdbcTemplate.update(
                "insert into dream_entry(id, user_id, date, title, content) values (?,?,?,?,?)",
                UUID.randomUUID(), user.getId(), LocalDate.now(), null, "content"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
