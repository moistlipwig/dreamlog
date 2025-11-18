package pl.kalin.dreamlog.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import lombok.Data;

/**
 * Configuration for Google AI Studio native API.
 * <p>
 * Used for image generation with Gemini 2.0 Flash, which requires native API
 * (not OpenAI-compatible endpoint).
 * <p>
 * Properties: google.ai.api-key, google.ai.base-url, google.ai.image-model
 */
@Configuration
@ConfigurationProperties(prefix = "google.ai")
@Data
public class GoogleAiConfig {

    /**
     * Google AI Studio API key (same as GOOGLE_AI_API_KEY for Spring AI)
     */
    private String apiKey;

    /**
     * Base URL for native Gemini API (not OpenAI-compatible)
     * Default: https://generativelanguage.googleapis.com/v1beta
     */
    private String baseUrl = "https://generativelanguage.googleapis.com/v1beta";

    /**
     * Model for text analysis (currently using Spring AI ChatClient)
     */
    private String textModel = "gemini-2.5-flash";

    /**
     * Model for image generation
     * Default: gemini-2.0-flash-preview-image-generation
     * Note: Retires Oct 31, 2025 - migrate to gemini-2.5-flash-image
     */
    private String imageModel = "gemini-2.0-flash-preview-image-generation";

    /**
     * RestClient configured for Google AI Studio native API.
     */
    @Bean
    public RestClient googleAiRestClient() {
        return RestClient.builder()
            .baseUrl(baseUrl)
            .defaultHeader("Content-Type", "application/json")
            .build();
    }
}
