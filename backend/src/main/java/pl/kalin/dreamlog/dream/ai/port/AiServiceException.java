package pl.kalin.dreamlog.dream.ai.port;

/**
 * Exception thrown when AI service operations fail.
 * Wraps underlying errors (network failures, API errors, invalid responses).
 *
 * Handled by:
 * - Resilience4j retry/circuit breaker (fast-fail)
 * - db-scheduler task retry logic (long-term retries)
 */
public class AiServiceException extends RuntimeException {

    public AiServiceException(String message) {
        super(message);
    }

    public AiServiceException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Factory method for rate limit errors.
     */
    public static AiServiceException rateLimitExceeded(String details) {
        return new AiServiceException("AI service rate limit exceeded: " + details);
    }

    /**
     * Factory method for network/timeout errors.
     */
    public static AiServiceException networkError(Throwable cause) {
        return new AiServiceException("AI service network error", cause);
    }

    /**
     * Factory method for invalid API responses.
     */
    public static AiServiceException invalidResponse(String details) {
        return new AiServiceException("Invalid AI service response: " + details);
    }
}
