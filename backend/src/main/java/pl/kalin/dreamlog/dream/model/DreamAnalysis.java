package pl.kalin.dreamlog.dream.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.*;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "dream_analysis")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DreamAnalysis {
    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "dream_id")
    private DreamEntry dream;

    private LocalDateTime createdAt;

    private String summary;

    @ElementCollection
    @CollectionTable(name = "dream_analysis_tags", joinColumns = @JoinColumn(name = "dream_analysis_id"))
    @Column(name = "tag")
    @Builder.Default
    private List<String> tags = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "dream_analysis_entities", joinColumns = @JoinColumn(name = "dream_analysis_id"))
    @Column(name = "entity")
    @Builder.Default
    private List<String> entities = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Double> emotions = new HashMap<>();

    @Column(columnDefinition = "text")
    private String interpretation;

    private Double riskScore;

    private Boolean recurring;

    private String language;

    private String style;

    private String modelVersion;
}
