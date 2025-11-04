package pl.kalin.dreamlog.dream.events;

import java.util.UUID;

/**
 * Domain event published when dream text analysis successfully completes.
 * Triggers image generation task.
 *
 * Event flow:
 * 1. AnalyzeTextTask completes successfully
 * 2. DreamAnalysis entity saved
 * 3. Dream state updated to TEXT_ANALYZED
 * 4. This event published
 * 5. Schedules GenerateImageTask via db-scheduler
 */
public record TextAnalysisCompletedEvent(
    UUID dreamId,
    UUID analysisId,
    String analysisSummary  // Used as prompt for image generation
) {
    public static TextAnalysisCompletedEvent of(UUID dreamId, UUID analysisId, String summary) {
        return new TextAnalysisCompletedEvent(dreamId, analysisId, summary);
    }
}
