package pl.kalin.dreamlog.dream.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import pl.kalin.dreamlog.user.User;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
}
