package pl.kalin.dreamlog.dream.ai.adapter;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import pl.kalin.dreamlog.dream.ai.port.AiServiceException;
import pl.kalin.dreamlog.dream.ai.port.DreamAnalysisAiService;
import pl.kalin.dreamlog.dream.ai.port.dto.AnalysisResult;
import pl.kalin.dreamlog.dream.ai.port.dto.ImageGenerationResult;

import java.util.*;

/**
 * Google AI Studio adapter for DreamAnalysisAiService port.
 * Uses Gemini Flash for text analysis and Imagen 3 for image generation.
 *
 * Resilience:
 * - @CircuitBreaker: Opens after 5 failures, half-open after 1min
 * - @Retry: 3 attempts with exponential backoff (2s, 4s, 8s)
 * - @RateLimiter: Max 10 requests/second
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GoogleAiStudioAdapter implements DreamAnalysisAiService {

    private final RestTemplate googleAiRestTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${google.ai.api-key}")
    private String apiKey;

    @Value("${google.ai.base-url}")
    private String baseUrl;

    @Value("${google.ai.text-model}")
    private String textModel;

    @Value("${google.ai.image-model}")
    private String imageModel;

    @Override
    @CircuitBreaker(name = "googleAi", fallbackMethod = "analyzeTextFallback")
    @Retry(name = "googleAi")
    @RateLimiter(name = "googleAi")
    public AnalysisResult analyzeText(String dreamContent) {
        log.info("Analyzing dream text with Google AI (model={})", textModel);

        try {
            String prompt = buildTextAnalysisPrompt(dreamContent);
            String url = String.format("%s/models/%s:generateContent?key=%s", baseUrl, textModel, apiKey);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                    Map.of("parts", List.of(
                        Map.of("text", prompt)
                    ))
                )
            );

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = googleAiRestTemplate.exchange(
                url,
                HttpMethod.POST,
                request,
                String.class
            );

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw AiServiceException.invalidResponse("Invalid response from Google AI: " + response.getStatusCode());
            }

            return parseTextAnalysisResponse(response.getBody());

        } catch (RestClientException e) {
            log.error("Network error calling Google AI text analysis", e);
            throw AiServiceException.networkError(e);
        } catch (Exception e) {
            log.error("Unexpected error during text analysis", e);
            throw new AiServiceException("Text analysis failed", e);
        }
    }

    @Override
    @CircuitBreaker(name = "googleAi", fallbackMethod = "generateImageFallback")
    @Retry(name = "googleAi")
    @RateLimiter(name = "googleAi")
    public ImageGenerationResult generateImage(String prompt) {
        log.info("Generating image with Google AI (model={})", imageModel);

        try {
            String imagePrompt = buildImageGenerationPrompt(prompt);
            String url = String.format("%s/models/%s:predict?key=%s", baseUrl, imageModel, apiKey);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> requestBody = Map.of(
                "instances", List.of(
                    Map.of("prompt", imagePrompt)
                ),
                "parameters", Map.of(
                    "sampleCount", 1
                )
            );

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = googleAiRestTemplate.exchange(
                url,
                HttpMethod.POST,
                request,
                String.class
            );

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw AiServiceException.invalidResponse("Invalid response from Google AI: " + response.getStatusCode());
            }

            return parseImageGenerationResponse(response.getBody());

        } catch (RestClientException e) {
            log.error("Network error calling Google AI image generation", e);
            throw AiServiceException.networkError(e);
        } catch (Exception e) {
            log.error("Unexpected error during image generation", e);
            throw new AiServiceException("Image generation failed", e);
        }
    }

    /**
     * Builds prompt for text analysis (Gemini Flash).
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
            """, dreamContent);
    }

    /**
     * Builds prompt for image generation (Imagen 3).
     */
    private String buildImageGenerationPrompt(String analysisSummary) {
        return String.format("""
            Create a dreamlike, surreal image based on this dream:

            %s

            Style: Ethereal, slightly surreal, soft lighting, dreamlike color palette (pastels, deep blues, purples).
            Mood: Evocative and mysterious.
            Quality: High detail, digital art style.
            """, analysisSummary);
    }

    /**
     * Parses Gemini Flash text analysis response.
     */
    private AnalysisResult parseTextAnalysisResponse(String responseBody) {
        try {
            // Parse Google AI response format
            Map<String, Object> response = objectMapper.readValue(responseBody, Map.class);

            // Extract text from response (Google AI response structure)
            List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
            if (candidates == null || candidates.isEmpty()) {
                throw AiServiceException.invalidResponse("No candidates in response");
            }

            Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
            List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
            String textResponse = (String) parts.get(0).get("text");

            // Parse JSON from text response
            textResponse = textResponse.replaceAll("```json\\s*", "").replaceAll("```\\s*", "").trim();
            Map<String, Object> analysisJson = objectMapper.readValue(textResponse, Map.class);

            return new AnalysisResult(
                (String) analysisJson.get("summary"),
                (List<String>) analysisJson.getOrDefault("tags", List.of()),
                (List<String>) analysisJson.getOrDefault("entities", List.of()),
                (Map<String, Double>) analysisJson.getOrDefault("emotions", Map.of()),
                (String) analysisJson.get("interpretation"),
                textModel
            );

        } catch (JsonProcessingException e) {
            log.error("Failed to parse text analysis response: {}", responseBody, e);
            throw AiServiceException.invalidResponse("Failed to parse AI response: " + e.getMessage());
        }
    }

    /**
     * Parses Imagen 3 image generation response.
     */
    private ImageGenerationResult parseImageGenerationResponse(String responseBody) {
        try {
            Map<String, Object> response = objectMapper.readValue(responseBody, Map.class);

            // Extract image data (base64 encoded)
            List<Map<String, Object>> predictions = (List<Map<String, Object>>) response.get("predictions");
            if (predictions == null || predictions.isEmpty()) {
                throw AiServiceException.invalidResponse("No predictions in response");
            }

            String base64Image = (String) predictions.get(0).get("bytesBase64Encoded");
            byte[] imageBytes = Base64.getDecoder().decode(base64Image);

            // Default dimensions (Imagen typically returns 1024x1024)
            int width = 1024;
            int height = 1024;

            return new ImageGenerationResult(
                imageBytes,
                "image/jpeg",
                width,
                height,
                imageModel
            );

        } catch (Exception e) {
            log.error("Failed to parse image generation response: {}", responseBody, e);
            throw AiServiceException.invalidResponse("Failed to parse image response: " + e.getMessage());
        }
    }

    /**
     * Fallback method for text analysis (circuit breaker open).
     */
    private AnalysisResult analyzeTextFallback(String dreamContent, Throwable t) {
        log.error("Text analysis fallback triggered", t);
        throw new AiServiceException("AI service unavailable (circuit breaker open)", t);
    }

    /**
     * Fallback method for image generation (circuit breaker open).
     */
    private ImageGenerationResult generateImageFallback(String prompt, Throwable t) {
        log.error("Image generation fallback triggered", t);
        throw new AiServiceException("AI service unavailable (circuit breaker open)", t);
    }
}
