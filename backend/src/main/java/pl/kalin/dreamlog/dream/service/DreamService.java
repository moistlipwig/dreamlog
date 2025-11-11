package pl.kalin.dreamlog.dream.service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import pl.kalin.dreamlog.dream.dto.DreamCreateRequest;
import pl.kalin.dreamlog.dream.dto.DreamResponse;
import pl.kalin.dreamlog.dream.dto.DreamUpdateRequest;
import pl.kalin.dreamlog.dream.events.DreamCreatedEvent;
import pl.kalin.dreamlog.dream.model.DreamAnalysis;
import pl.kalin.dreamlog.dream.model.DreamEntry;
import pl.kalin.dreamlog.dream.repository.DreamAnalysisRepository;
import pl.kalin.dreamlog.dream.repository.DreamEntryRepository;
import pl.kalin.dreamlog.user.User;

/**
 * Service for managing dream entries with user-based authorization.
 * Ensures that users can only access and modify their own dreams.
 * Publishes domain events for async AI processing pipeline.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class DreamService {

    private final DreamEntryRepository dreamRepository;
    private final DreamAnalysisRepository analysisRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Get paginated dreams for the authenticated user.
     *
     * @param user     the authenticated user
     * @param pageable pagination parameters (page, size, sort)
     * @return page of user's dreams
     */
    @Transactional(readOnly = true)
    public Page<DreamResponse> getUserDreams(User user, Pageable pageable) {
        log.debug("Fetching dreams for user: {} with pagination: {}", user.getEmail(), pageable);
        return dreamRepository.findByUserId(user.getId(), pageable)
            .map(DreamResponse::from);
    }

    /**
     * Get all dreams for the authenticated user (unpaginated).
     *
     * @param user the authenticated user
     * @return list of user's dreams (empty if no dreams found)
     */
    @Transactional(readOnly = true)
    public List<DreamResponse> getUserDreams(User user) {
        log.debug("Fetching all dreams for user: {}", user.getEmail());
        return dreamRepository.findByUserId(user.getId())
            .stream()
            .map(DreamResponse::from)
            .toList();
    }

    /**
     * Get a single dream by ID, only if it belongs to the authenticated user.
     * Includes analysis and image data when available.
     *
     * @param user    the authenticated user
     * @param dreamId the dream ID
     * @return dream response with analysis and image
     * @throws AccessDeniedException if dream not found or doesn't belong to user
     */
    @Transactional(readOnly = true)
    public DreamResponse getDreamById(User user, UUID dreamId) {
        log.debug("Fetching dream {} for user: {}", dreamId, user.getEmail());
        DreamEntry dream = dreamRepository.findByIdAndUserId(dreamId, user.getId())
            .orElseThrow(() -> new AccessDeniedException("Dream not found or access denied"));

        // Load analysis if available
        DreamAnalysis analysis = analysisRepository.findByDreamId(dreamId).orElse(null);

        return DreamResponse.from(dream, analysis);
    }

    /**
     * Create a new dream entry for the authenticated user.
     */
    public UUID createDream(User user, DreamCreateRequest request) {
        log.debug("Creating dream for user: {}", user.getEmail());

        // Auto-generate title from content if not provided (domain logic)
        String title = (request.title() == null || request.title().isBlank())
            ? DreamEntry.generateTitleFromContent(request.content())
            : request.title();

        DreamEntry dream = DreamEntry.builder()
            .user(user)
            .date(request.date())
            .title(title)
            .content(request.content())
            .moodInDream(request.moodInDream())
            .moodAfterDream(request.moodAfterDream())
            .vividness(request.vividness() != null ? request.vividness() : 0)
            .lucid(request.lucid() != null ? request.lucid() : false)
            // Wrap in ArrayList to ensure mutability for Hibernate
            .tags(request.tags() != null ? new ArrayList<>(request.tags()) : new ArrayList<>())
            .build();

        DreamEntry saved = dreamRepository.save(dream);
        log.info("Created dream {} for user {}", saved.getId(), user.getEmail());

        // Publish event to trigger async AI analysis pipeline (AFTER_COMMIT)
        eventPublisher.publishEvent(DreamCreatedEvent.of(
            saved.getId(),
            user.getId(),
            saved.getContent()
        ));

        log.debug("DreamCreatedEvent published for dreamId={}", saved.getId());

        return saved.getId();
    }

    /**
     * Update an existing dream (PUT - full replacement).
     * Only the owner can update their dream.
     */
    public void updateDream(User user, UUID dreamId, DreamUpdateRequest request) {
        log.debug("Updating dream {} for user: {}", dreamId, user.getEmail());

        DreamEntry dream = dreamRepository.findByIdAndUserId(dreamId, user.getId())
            .orElseThrow(() -> new AccessDeniedException("Dream not found or access denied"));

        // Delegate to domain model (encapsulates update logic and defaults)
        dream.updateFrom(
            request.date(),
            request.title(),
            request.content(),
            request.moodInDream(),
            request.moodAfterDream(),
            request.vividness(),
            request.lucid(),
            request.tags()
        );

        dreamRepository.save(dream);
        log.info("Updated dream {} for user {}", dreamId, user.getEmail());
    }

    /**
     * Delete a dream entry.
     * Only the owner can delete their dream.
     *
     * @param user    the authenticated user
     * @param dreamId the dream ID
     * @throws AccessDeniedException if dream not found or doesn't belong to user
     */
    public void deleteDream(User user, UUID dreamId) {
        log.debug("Deleting dream {} for user: {}", dreamId, user.getEmail());

        DreamEntry dream = dreamRepository.findByIdAndUserId(dreamId, user.getId())
            .orElseThrow(() -> new AccessDeniedException("Dream not found or access denied"));

        dreamRepository.delete(dream);
        log.info("Deleted dream {} for user {}", dreamId, user.getEmail());
    }

    /**
     * Search dreams using full-text search with fuzzy fallback.
     * Strategy: Try PostgreSQL FTS first (fast, exact matching), then fuzzy search if no results.
     * <p>
     * Search supports:
     * - Natural language queries: "lucid dream"
     * - Boolean operators: "flying -nightmare"
     * - Phrase search: "\"flying car\""
     * - Polish characters: "łódź" matches "lodz"
     * - Typo tolerance: "lucdi" matches "lucid" (fuzzy fallback)
     *
     * @param user  the authenticated user
     * @param query search query string (minimum 3 characters)
     * @return list of matching dreams ordered by relevance (max 100 results)
     */
    @Transactional(readOnly = true)
    public List<DreamResponse> searchDreams(User user, String query) {
        log.debug("Searching dreams for user {} with query: {}", user.getEmail(), query);

        // Try full-text search first (fast, PostgreSQL FTS with websearch_to_tsquery)
        List<DreamEntry> results = dreamRepository.searchByFullText(user.getId(), query);

        // Fallback to fuzzy search if no FTS results (handles typos)
        if (results.isEmpty()) {
            log.debug("No FTS results, trying fuzzy search for query: {}", query);
            results = dreamRepository.searchByFuzzy(user.getId(), query);
        }

        log.debug("Found {} dreams for query: {}", results.size(), query);
        return results.stream()
            .map(DreamResponse::from)
            .toList();
    }
}
