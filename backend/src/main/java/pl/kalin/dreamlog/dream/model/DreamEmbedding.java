package pl.kalin.dreamlog.dream.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "dream_embedding")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DreamEmbedding {
    @Id
    private UUID dreamId;

    @OneToOne
    @MapsId
    @JoinColumn(name = "dream_id")
    private DreamEntry dream;

}
