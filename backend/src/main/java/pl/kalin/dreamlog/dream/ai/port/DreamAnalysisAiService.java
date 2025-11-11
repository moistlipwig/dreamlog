package pl.kalin.dreamlog.dream.ai.port;

import pl.kalin.dreamlog.dream.ai.port.dto.AnalysisResult;
import pl.kalin.dreamlog.dream.ai.port.dto.ImageGenerationResult;

/**
 * Port (interface) for AI-powered dream analysis and image generation.
 * Follows Hexagonal Architecture - domain defines what it needs, adapter provides implementation.
 *
 * Implementations:
 * - GoogleAiStudioAdapter (primary adapter, uses Gemini Flash + Imagen 3)
 * - Future: OpenAiAdapter, ClaudeAdapter, LocalModelAdapter
 *
 * Benefits:
 * - Swappable AI providers without changing domain code
 * - Easy mocking in unit tests
 * - No vendor lock-in
 */
public interface DreamAnalysisAiService {

    /**
     * Analyzes dream text content using AI (LLM).
     *
     * @param dreamContent the user's dream description
     * @return analysis result with summary, tags, emotions, interpretation
     * @throws AiServiceException if AI API call fails (network error, rate limit, invalid response)
     */
    AnalysisResult analyzeText(String dreamContent);

    /**
     * Generates dream image based on analysis summary using AI (image generation model).
     *
     * @param prompt the image generation prompt (usually analysis summary + style instructions)
     * @return image generation result with image bytes and metadata
     * @throws AiServiceException if AI API call fails or image generation fails
     */
    ImageGenerationResult generateImage(String prompt);
}
