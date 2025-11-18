package pl.kalin.dreamlog.dream.model;

/**
 * Processing state for async AI pipeline (text analysis + image generation).
 * State transitions:
 * CREATED → ANALYZING_TEXT → TEXT_ANALYZED → GENERATING_IMAGE → COMPLETED
 *   ↓         ↓                ↓               ↓
 *   └─────────┴────────────────┴───────────────→ FAILED (after max retries)
 */
public enum DreamProcessingState {
    /**
     * Initial state after dream creation via POST /api/dreams.
     * Waiting for text analysis task to be scheduled.
     */
    CREATED,

    /**
     * Text analysis task (AnalyzeTextTask) is currently running.
     * AI is analyzing dream content for emotions, tags, entities, interpretation.
     */
    ANALYZING_TEXT,

    /**
     * Text analysis completed successfully, DreamAnalysis entity saved.
     * Waiting for image generation task to be scheduled.
     */
    TEXT_ANALYZED,

    /**
     * Image generation task (GenerateImageTask) is currently running.
     * AI is generating dream image based on analysis summary.
     */
    GENERATING_IMAGE,

    /**
     * All processing complete: analysis saved, image generated and stored.
     * Dream is ready for user consumption with full AI insights.
     */
    COMPLETED,

    /**
     * Unrecoverable failure after maximum retry attempts (8 retries).
     * failure_reason column contains error details.
     */
    FAILED
}
