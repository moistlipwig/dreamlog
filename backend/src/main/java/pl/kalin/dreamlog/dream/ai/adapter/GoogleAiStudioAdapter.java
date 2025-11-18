package pl.kalin.dreamlog.dream.ai.adapter;

import java.util.List;
import java.util.Map;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import pl.kalin.dreamlog.dream.ai.port.AiServiceException;
import pl.kalin.dreamlog.dream.ai.port.DreamAnalysisAiService;
import pl.kalin.dreamlog.dream.ai.port.dto.AnalysisResult;
import pl.kalin.dreamlog.dream.ai.port.dto.ImageGenerationResult;

/**
 * Spring AI adapter for DreamAnalysisAiService using Google Gemini.
 * <p>
 * Leverages:
 * - Spring AI ChatClient for text analysis (OpenAI-compatible endpoint)
 * - GeminiImageGenerationClient for image generation (native Gemini API)
 * - Google AI Studio free tier (generativelanguage.googleapis.com)
 * - Resilience4j for circuit breaker, retry, and rate limiting
 * <p>
 * Configuration: See application.yml -> spring.ai.openai.* and google.ai.*
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GoogleAiStudioAdapter implements DreamAnalysisAiService {

    private final ChatModel chatModel;
    private final GeminiImageGenerationClient imageGenerationClient;

    @Override
    @CircuitBreaker(name = "googleAi", fallbackMethod = "analyzeTextFallback")
    @Retry(name = "googleAi")
    @RateLimiter(name = "googleAi")
    public AnalysisResult analyzeText(String dreamContent) {
        log.info("Analyzing dream text with Spring AI + Gemini");

        try {
            String prompt = buildTextAnalysisPrompt(dreamContent);

            // Spring AI ChatClient with structured output
            ChatClient chatClient = ChatClient.create(chatModel);

            DreamAnalysisResponse response = chatClient.prompt()
                .user(prompt)
                .call()
                .entity(DreamAnalysisResponse.class);

            // Get model name from chat model
            String modelVersion = chatModel.getDefaultOptions().getModel();

            return new AnalysisResult(
                response.summary(),
                response.tags(),
                response.entities(),
                response.emotions(),
                response.interpretation(),
                modelVersion
            );

        } catch (Exception e) {
            log.error("Error during dream text analysis", e);
            throw new AiServiceException("Failed to analyze dream text", e);
        }
    }

    @Override
    @CircuitBreaker(name = "googleAi", fallbackMethod = "generateImageFallback")
    @Retry(name = "googleAi")
    @RateLimiter(name = "googleAi")
    public ImageGenerationResult generateImage(String prompt) {
        log.info("Generating image with Gemini 2.0 Flash via native API");

        return imageGenerationClient.generateImage(prompt);
    }

    /**
     * Builds prompt for structured JSON output from Gemini.
     * Uses clear instructions for consistent parsing.
     */
    private String buildTextAnalysisPrompt(String dreamContent) {
        return String.format("""
            You are an expert dream analyst specializing in symbolism, emotions, and psychological interpretation.

            **Dream Description:**
            %s

            **Task:** Analyze this dream and return ONLY a valid JSON response (no markdown, no extra text) with this exact structure:

            {
              "summary": "Brief 1-2 sentence summary of the dream",
              "tags": ["tag1", "tag2", "tag3"],
              "entities": ["entity1", "entity2"],
              "emotions": {
                "joy": 0.0,
                "fear": 0.0,
                "anger": 0.0,
                "sadness": 0.0,
                "surprise": 0.0
              },
              "interpretation": "Detailed psychological interpretation focusing on symbolism and meaning"
            }

            **Guidelines:**
            - summary: Max 100 words
            - tags: Max 10 lowercase tags (e.g., "flying", "water", "fear")
            - entities: Key people, places, objects
            - emotions: Scores 0.0-1.0, should sum to ~1.0
            - interpretation: Insightful but not prescriptive (2-3 sentences)

            Return ONLY the JSON object, nothing else.
            """, dreamContent);
    }

    /**
     * Record for structured output from Gemini.
     * Spring AI automatically deserializes JSON response to this record.
     */
    private record DreamAnalysisResponse(
        String summary,
        List<String> tags,
        List<String> entities,
        Map<String, Double> emotions,
        String interpretation
    ) {
    }

    /**
     * Fallback method for text analysis (circuit breaker open).
     */
    @SuppressWarnings("unused")
    private AnalysisResult analyzeTextFallback(String dreamContent, Throwable t) {
        log.error("Text analysis fallback triggered", t);
        throw new AiServiceException("AI service unavailable (circuit breaker open)", t);
    }

    /**
     * Fallback method for image generation (circuit breaker open).
     */
    @SuppressWarnings("unused")
    private ImageGenerationResult generateImageFallback(String prompt, Throwable t) {
        log.error("Image generation fallback triggered", t);
        throw new AiServiceException("AI service unavailable (circuit breaker open)", t);
    }
}
