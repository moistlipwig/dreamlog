package pl.kalin.dreamlog.dream.ai.port.dto;

import java.util.List;
import java.util.Map;

/**
 * Result of AI-powered dream text analysis.
 * Contains structured insights extracted from dream content by LLM (e.g., Gemini Flash).
 */
public record AnalysisResult(
    String summary,                 // Brief 1-2 sentence summary of the dream
    List<String> tags,              // Thematic tags (e.g., "flying", "water", "fear")
    List<String> entities,          // Key people, places, objects mentioned
    Map<String, Double> emotions,   // Emotional intensity scores (0.0-1.0), e.g., {"joy": 0.7, "fear": 0.3}
    String interpretation,          // Detailed psychological interpretation
    String modelVersion             // AI model used (e.g., "gemini-1.5-flash-latest")
) {
    /**
     * Get the primary emotion (highest intensity score).
     * Used for image generation prompt mood.
     */
    public String getPrimaryEmotion() {
        return emotions.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse("neutral");
    }
}
