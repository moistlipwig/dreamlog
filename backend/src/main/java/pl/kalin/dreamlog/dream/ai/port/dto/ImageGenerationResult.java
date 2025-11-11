package pl.kalin.dreamlog.dream.ai.port.dto;

/**
 * Result of AI-powered dream image generation.
 * Contains image data and metadata from image generation model (e.g., Imagen 3).
 */
public record ImageGenerationResult(
    byte[] imageData,           // Raw image bytes (JPEG format)
    String mimeType,            // MIME type (e.g., "image/jpeg")
    int width,                  // Image width in pixels
    int height,                 // Image height in pixels
    String modelVersion         // AI model used (e.g., "imagen-3.0-generate-001")
) {
    /**
     * Generate suggested filename based on dimensions and format.
     */
    public String suggestFilename(String prefix) {
        String extension = mimeType.equals("image/jpeg") ? "jpg" : "png";
        return String.format("%s_%dx%d.%s", prefix, width, height, extension);
    }
}
