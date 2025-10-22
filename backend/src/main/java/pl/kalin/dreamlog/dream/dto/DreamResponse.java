package pl.kalin.dreamlog.dream.dto;

import pl.kalin.dreamlog.dream.model.DreamEntry;
import pl.kalin.dreamlog.dream.model.Mood;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO for dream entry.
 * Does not include user information to avoid circular references and data leakage.
 */
public record DreamResponse(
    UUID id,
    LocalDate date,
    String title,
    String content,
    Mood moodInDream,
    Mood moodAfterDream,
    Integer vividness,
    Boolean lucid,
    List<String> tags
) {
    /**
     * Factory method to create DreamResponse from DreamEntry entity.
     * @param entity the DreamEntry entity
     * @return DreamResponse DTO
     */
    public static DreamResponse from(DreamEntry entity) {
        return new DreamResponse(
            entity.getId(),
            entity.getDate(),
            entity.getTitle(),
            entity.getContent(),
            entity.getMoodInDream(),
            entity.getMoodAfterDream(),
            entity.getVividness(),
            entity.isLucid(),
            entity.getTags()
        );
    }
}
