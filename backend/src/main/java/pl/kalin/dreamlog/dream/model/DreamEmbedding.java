package pl.kalin.dreamlog.dream.model;

import com.pgvector.PGvector;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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

    @JdbcTypeCode(SqlTypes.OTHER)
    @Column(columnDefinition = "vector(1536)")
    private PGvector vector;
}
