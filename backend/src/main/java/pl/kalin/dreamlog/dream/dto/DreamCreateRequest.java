package pl.kalin.dreamlog.dream.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import pl.kalin.dreamlog.dream.model.Mood;

import java.time.LocalDate;
import java.util.List;

/**
 * Request to create a new dream entry.
 */
public record DreamCreateRequest(
    @NotNull(message = "Date is required")
    LocalDate date,

    @NotBlank(message = "Title is required")
    String title,

    @NotBlank(message = "Content is required")
    String content,

    Mood moodInDream,

    Mood moodAfterDream,

    Integer vividness,

    Boolean lucid,

    List<String> tags
) {}
