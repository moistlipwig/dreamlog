package pl.kalin.dreamlog.dream.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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

    @Size(max = 255, message = "Title must not exceed 255 characters")
    String title, // Optional - auto-generated if null/blank

    @NotBlank(message = "Content is required")
    @Size(max = 10000, message = "Content must not exceed 10,000 characters (LLM token limits)")
    String content,

    Mood moodInDream,

    Mood moodAfterDream,

    Integer vividness,

    Boolean lucid,

    @Size(max = 20, message = "Maximum 20 tags allowed")
    List<String> tags
) {}
