package pl.kalin.dreamlog.dream.ai.adapter;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import pl.kalin.dreamlog.config.GoogleAiConfig;
import pl.kalin.dreamlog.dream.ai.port.AiServiceException;
import pl.kalin.dreamlog.dream.ai.port.dto.ImageGenerationResult;

/**
 * Client for Gemini native API image generation.
 * <p>
 * Uses native Gemini API (not OpenAI-compatible) to generate images with
 * gemini-2.0-flash-preview-image-generation model.
 * <p>
 * API Documentation: https://ai.google.dev/gemini-api/docs/image-generation
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GeminiImageGenerationClient {

    private final RestClient googleAiRestClient;
    private final GoogleAiConfig googleAiConfig;

    /**
     * Generates an image from text prompt using Gemini native API.
     *
     * @param prompt Text description of desired image
     * @return ImageGenerationResult with base64-encoded image data
     * @throws AiServiceException if image generation fails
     */
    public ImageGenerationResult generateImage(String prompt) {
        log.info("Generating image with Gemini native API: {}", googleAiConfig.getImageModel());

        try {
            // Build request payload
            Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                    Map.of("parts", List.of(
                        Map.of("text", prompt)
                    ))
                ),
                "generationConfig", Map.of(
                    "responseModalities", List.of("TEXT", "IMAGE")
                )
            );

            // Call Gemini API
            String endpoint = String.format(
                "/models/%s:generateContent?key=%s",
                googleAiConfig.getImageModel(),
                googleAiConfig.getApiKey()
            );

            GeminiImageResponse response = googleAiRestClient
                .post()
                .uri(endpoint)
                .body(requestBody)
                .retrieve()
                .body(GeminiImageResponse.class);

            if (response == null || response.candidates() == null || response.candidates().isEmpty()) {
                throw new AiServiceException("Empty response from Gemini image generation API");
            }

            // Extract image from response
            byte[] imageData = extractImageData(response);

            // Determine image dimensions
            int[] dimensions = getImageDimensions(imageData);

            return new ImageGenerationResult(
                imageData,
                "image/png",  // Gemini returns PNG format
                dimensions[0],  // width
                dimensions[1],  // height
                googleAiConfig.getImageModel()
            );

        } catch (Exception e) {
            log.error("Failed to generate image with Gemini", e);
            throw new AiServiceException("Image generation failed: " + e.getMessage(), e);
        }
    }

    /**
     * Extracts base64-encoded image data from Gemini API response.
     * <p>
     * Response structure:
     * <pre>
     * {
     *   "candidates": [{
     *     "content": {
     *       "parts": [
     *         {"text": "Generated image description"},
     *         {"inlineData": {"mimeType": "image/png", "data": "base64..."}}
     *       ]
     *     }
     *   }]
     * }
     * </pre>
     */
    private byte[] extractImageData(GeminiImageResponse response) {
        return response.candidates().stream()
            .flatMap(candidate -> candidate.content().parts().stream())
            .filter(part -> part.inlineData() != null)
            .findFirst()
            .map(part -> Base64.getDecoder().decode(part.inlineData().data()))
            .orElseThrow(() -> new AiServiceException("No image found in Gemini API response"));
    }

    /**
     * Extracts image dimensions from byte array.
     *
     * @param imageData Raw image bytes
     * @return Array with [width, height]
     */
    private int[] getImageDimensions(byte[] imageData) {
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageData));
            if (image == null) {
                log.warn("Could not read image dimensions, using default 1024x1024");
                return new int[]{1024, 1024};
            }
            return new int[]{image.getWidth(), image.getHeight()};
        } catch (Exception e) {
            log.warn("Error reading image dimensions, using default 1024x1024", e);
            return new int[]{1024, 1024};
        }
    }

    /**
     * Records for Gemini native API response structure.
     */
    private record GeminiImageResponse(
        List<Candidate> candidates
    ) {
    }

    private record Candidate(
        Content content
    ) {
    }

    private record Content(
        List<Part> parts
    ) {
    }

    private record Part(
        String text,
        InlineData inlineData
    ) {
    }

    private record InlineData(
        String mimeType,
        String data  // base64-encoded image
    ) {
    }
}
