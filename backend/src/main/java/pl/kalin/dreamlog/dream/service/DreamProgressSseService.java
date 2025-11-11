package pl.kalin.dreamlog.dream.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import pl.kalin.dreamlog.dream.model.DreamProcessingState;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-Sent Events (SSE) service for real-time dream processing updates.
 * Allows frontend to receive notifications when dream analysis completes.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DreamProgressSseService {

    // Map of dreamId -> list of SSE emitters listening for that dream
    private final Map<UUID, SseEmitter> emitters = new ConcurrentHashMap<>();

    /**
     * Creates an SSE connection for a specific dream.
     * Frontend can listen to this to get real-time updates.
     *
     * @param dreamId the dream ID to listen for
     * @return SSE emitter
     */
    public SseEmitter createEmitter(UUID dreamId) {
        SseEmitter emitter = new SseEmitter(300000L);  // 5 minute timeout

        emitter.onCompletion(() -> {
            log.debug("SSE emitter completed for dream: {}", dreamId);
            emitters.remove(dreamId);
        });

        emitter.onTimeout(() -> {
            log.debug("SSE emitter timeout for dream: {}", dreamId);
            emitters.remove(dreamId);
        });

        emitter.onError((ex) -> {
            log.error("SSE emitter error for dream: {}", dreamId, ex);
            emitters.remove(dreamId);
        });

        emitters.put(dreamId, emitter);
        log.info("SSE emitter created for dream: {}", dreamId);

        return emitter;
    }

    /**
     * Sends a progress update to all listeners of a dream.
     *
     * @param dreamId the dream ID
     * @param state the current processing state
     * @param message optional message
     */
    public void sendProgress(UUID dreamId, DreamProcessingState state, String message) {
        SseEmitter emitter = emitters.get(dreamId);
        if (emitter != null) {
            try {
                Map<String, Object> event = Map.of(
                    "dreamId", dreamId.toString(),
                    "state", state.toString(),
                    "message", message != null ? message : ""
                );

                emitter.send(SseEmitter.event()
                    .name("progress")
                    .data(event));

                log.debug("Sent SSE progress for dream {}: state={}", dreamId, state);

                // If completed or failed, close the emitter
                if (state == DreamProcessingState.COMPLETED || state == DreamProcessingState.FAILED) {
                    emitter.complete();
                    emitters.remove(dreamId);
                }

            } catch (IOException e) {
                log.error("Failed to send SSE event for dream: {}", dreamId, e);
                emitters.remove(dreamId);
            }
        }
    }
}
