package pl.kalin.dreamlog.dream.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import pl.kalin.dreamlog.user.User;

@Entity
@Table(name = "dream_entry")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DreamEntry {
    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    @NotNull
    private User user;

    @NotNull
    private LocalDate date;

    @NotBlank
    private String title;

    @Column(columnDefinition = "text")
    @NotBlank
    private String content;

    @Enumerated(EnumType.STRING)
    private Mood moodInDream;

    @Enumerated(EnumType.STRING)
    private Mood moodAfterDream;

    private int vividness;

    private boolean lucid;

    @ElementCollection
    @CollectionTable(name = "dream_entry_tags", joinColumns = @JoinColumn(name = "dream_entry_id"))
    @Column(name = "tag")
    @Builder.Default
    private List<String> tags = new ArrayList<>();

    /**
     * Full-text search vector (tsvector) maintained by database trigger.
     * Do not modify manually - automatically updated on INSERT/UPDATE by trigger.
     */
    @Column(name = "search_vector", columnDefinition = "tsvector", insertable = false, updatable = false)
    private String searchVector;


    public static String generateTitleFromContent(String content) {
        if (content == null || content.isBlank()) {
            return "Untitled Dream";
        }

        // Try to extract first sentence (up to first period, exclamation, or question mark)
        int firstSentenceEnd = content.length();
        int periodIdx = content.indexOf('.');
        int exclamIdx = content.indexOf('!');
        int questionIdx = content.indexOf('?');

        if (periodIdx > 0) {
            firstSentenceEnd = periodIdx;
        }
        if (exclamIdx > 0) {
            firstSentenceEnd = Math.min(firstSentenceEnd, exclamIdx);
        }
        if (questionIdx > 0) {
            firstSentenceEnd = Math.min(firstSentenceEnd, questionIdx);
        }

        String title = content.substring(0, firstSentenceEnd).trim();

        // If first sentence is too long, truncate to 50 chars
        if (title.length() > 50) {
            title = title.substring(0, 47).trim() + "...";
        }

        return title.isEmpty() ? "Untitled Dream" : title;
    }

    /**
     * Update all fields from request (PUT semantics - full replacement).
     * Domain logic: encapsulates update rules and defaults.
     *
     * @param date           dream date
     * @param title          dream title (can be null)
     * @param content        dream content
     * @param moodInDream    mood in dream
     * @param moodAfterDream mood after waking
     * @param vividness      vividness level (0-10)
     * @param lucid          whether dream was lucid
     * @param tags           list of tags
     */
    public void updateFrom(LocalDate date, String title, String content,
                           Mood moodInDream, Mood moodAfterDream,
                           Integer vividness, Boolean lucid, List<String> tags) {
        this.date = date;
        this.title = title;
        this.content = content;
        this.moodInDream = moodInDream;
        this.moodAfterDream = moodAfterDream;
        this.vividness = vividness != null ? vividness : 0;
        this.lucid = lucid != null ? lucid : false;
        // Wrap in ArrayList to ensure mutability for Hibernate
        this.tags = tags != null ? new ArrayList<>(tags) : new ArrayList<>();
    }
}
