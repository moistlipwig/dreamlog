package pl.kalin.dreamlog.dream.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import pl.kalin.dreamlog.dream.model.Mood;

import java.time.LocalDate;
import java.util.List;

/**
 * Request to create a new dream entry.
 * Title is optional - if not provided, it will be auto-generated from content.
 */
public record DreamCreateRequest(
    @NotNull(message = "Date is required")
    LocalDate date,

    String title, // Optional - auto-generated if null/blank

    @NotBlank(message = "Content is required")
    String content,

    Mood moodInDream,

    Mood moodAfterDream,

    Integer vividness,

    Boolean lucid,

    List<String> tags
) {}
