package pl.kalin.dreamlog.dream.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import pl.kalin.dreamlog.dream.model.DreamEntry;
import pl.kalin.dreamlog.dream.model.Mood;
import pl.kalin.dreamlog.dream.repository.DreamEntryRepository;
import pl.kalin.dreamlog.user.User;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DreamEntryController.class)
@AutoConfigureMockMvc(addFilters = false)
class DreamEntryControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DreamEntryRepository repository;

    @Test
    void shouldReturnAllDreams() throws Exception {
        User user = User.builder().id(UUID.randomUUID()).username("john").email("john@example.com").build();
        DreamEntry dream = DreamEntry.builder()
                .id(UUID.randomUUID())
                .user(user)
                .date(LocalDate.of(2024, 1, 1))
                .title("Title")
                .content("Content")
                .moodInDream(Mood.HAPPY)
                .build();
        given(repository.findAll()).willReturn(List.of(dream));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/dreams"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Title"))
                .andExpect(jsonPath("$[0].user.username").value("john"));
    }

    @Test
    void shouldValidateDreamOnCreate() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/api/dreams")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnNotFoundWhenMissing() throws Exception {
        UUID id = UUID.randomUUID();
        given(repository.findById(id)).willReturn(Optional.empty());

        mockMvc.perform(MockMvcRequestBuilders.get("/api/dreams/" + id))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldCreateDream() throws Exception {
        User user = User.builder().id(UUID.randomUUID()).username("john").email("john@example.com").build();
        DreamEntry dream = DreamEntry.builder()
                .id(UUID.randomUUID())
                .user(user)
                .date(LocalDate.of(2024, 1, 1))
                .title("Title")
                .content("Content")
                .build();
        given(repository.save(any(DreamEntry.class))).willReturn(dream);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/dreams")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dream)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Title"));

        verify(repository).save(any(DreamEntry.class));
    }
}
