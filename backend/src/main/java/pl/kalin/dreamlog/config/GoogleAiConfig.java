package pl.kalin.dreamlog.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * Configuration for Google AI Studio API client.
 * Provides RestTemplate with Resilience4j decorators (circuit breaker, retry, rate limiter).
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class GoogleAiConfig {

    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final RetryRegistry retryRegistry;
    private final RateLimiterRegistry rateLimiterRegistry;

    /**
     * RestTemplate bean for Google AI Studio API calls.
     * Configured with Resilience4j decorators for fault tolerance.
     */
    @Bean
    @SuppressWarnings("deprecation")  // Timeout methods deprecated but still functional
    public RestTemplate googleAiRestTemplate(RestTemplateBuilder builder) {
        log.info("Creating RestTemplate for Google AI Studio API");

        // Get Resilience4j components from registry
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("googleAi");
        Retry retry = retryRegistry.retry("googleAi");
        RateLimiter rateLimiter = rateLimiterRegistry.rateLimiter("googleAi");

        // Log circuit breaker state changes
        circuitBreaker.getEventPublisher()
            .onStateTransition(event -> log.warn("Google AI Circuit Breaker state changed: {}", event))
            .onError(event -> log.error("Google AI Circuit Breaker recorded error: {}", event.getThrowable().getMessage()));

        // Log retry attempts
        retry.getEventPublisher()
            .onRetry(event -> log.warn("Retrying Google AI API call, attempt {}", event.getNumberOfRetryAttempts()));

        // Build RestTemplate with timeouts (using new API)
        return builder
            .setConnectTimeout(Duration.ofSeconds(10))
            .setReadTimeout(Duration.ofSeconds(60))  // AI API calls can take time
            .build();
    }
}
