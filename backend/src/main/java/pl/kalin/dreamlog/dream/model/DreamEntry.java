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
}
