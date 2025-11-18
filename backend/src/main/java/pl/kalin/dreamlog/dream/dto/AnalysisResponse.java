package pl.kalin.dreamlog.dream.dto;

import pl.kalin.dreamlog.dream.model.DreamAnalysis;

import java.util.List;
import java.util.Map;

/**
 * Analysis data in dream response.
 * Nested within DreamResponse when analysis is available.
 */
public record AnalysisResponse(
    String summary,
    List<String> tags,
    List<String> entities,
    Map<String, Double> emotions,
    String interpretation
) {
    public static AnalysisResponse from(DreamAnalysis analysis) {
        if (analysis == null) {
            return null;
        }
        return new AnalysisResponse(
            analysis.getSummary(),
            analysis.getTags(),
            analysis.getEntities(),
            analysis.getEmotions(),
            analysis.getInterpretation()
        );
    }
}
