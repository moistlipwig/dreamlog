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
import pl.kalin.dreamlog.dream.dto.DreamCreateRequest;
import pl.kalin.dreamlog.dream.dto.DreamResponse;
import pl.kalin.dreamlog.dream.dto.DreamUpdateRequest;
import pl.kalin.dreamlog.dream.model.DreamEntry;
import pl.kalin.dreamlog.dream.repository.DreamEntryRepository;
import pl.kalin.dreamlog.user.User;

/**
 * Service for managing dream entries with user-based authorization.
 * Ensures that users can only access and modify their own dreams.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class DreamService {

    private final DreamEntryRepository dreamRepository;

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
     *
     * @param user    the authenticated user
     * @param dreamId the dream ID
     * @return dream response
     * @throws AccessDeniedException if dream not found or doesn't belong to user
     */
    @Transactional(readOnly = true)
    public DreamResponse getDreamById(User user, UUID dreamId) {
        log.debug("Fetching dream {} for user: {}", dreamId, user.getEmail());
        DreamEntry dream = dreamRepository.findByIdAndUserId(dreamId, user.getId())
            .orElseThrow(() -> new AccessDeniedException("Dream not found or access denied"));
        return DreamResponse.from(dream);
    }

    /**
     * Create a new dream entry for the authenticated user.
     *
     * @param user    the authenticated user (becomes owner of the dream)
     * @param request the dream data
     * @return created dream response
     */
    public DreamResponse createDream(User user, DreamCreateRequest request) {
        log.debug("Creating dream for user: {}", user.getEmail());

        // Auto-generate title from content if not provided
        String title = (request.title() == null || request.title().isBlank())
            ? generateTitleFromContent(request.content())
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
        return DreamResponse.from(saved);
    }

    /**
     * Generate a title from the first sentence or first 50 characters of content.
     *
     * @param content the dream content
     * @return generated title
     */
    private String generateTitleFromContent(String content) {
        if (content == null || content.isBlank()) {
            return "Untitled Dream";
        }

        // Try to extract first sentence (up to first period, exclamation, or question mark)
        int firstSentenceEnd = content.length();
        int periodIdx = content.indexOf('.');
        int exclamIdx = content.indexOf('!');
        int questionIdx = content.indexOf('?');

        if (periodIdx > 0) {
            firstSentenceEnd = Math.min(firstSentenceEnd, periodIdx);
        }
        if (exclamIdx > 0) {
            firstSentenceEnd = Math.min(firstSentenceEnd, exclamIdx);
        }
        if (questionIdx > 0) {
            firstSentenceEnd = Math.min(firstSentenceEnd, questionIdx);
        }

        String title = content.substring(0, firstSentenceEnd).trim();

        // If first sentence is too long, truncate to 50 chars
        if (title.length() > 50) {
            title = title.substring(0, 47).trim() + "...";
        }

        return title.isEmpty() ? "Untitled Dream" : title;
    }

    /**
     * Update an existing dream (PUT - full replacement).
     * Only the owner can update their dream.
     *
     * @param user    the authenticated user
     * @param dreamId the dream ID
     * @param request the updated dream data
     * @return updated dream response
     * @throws AccessDeniedException if dream not found or doesn't belong to user
     */
    public DreamResponse updateDream(User user, UUID dreamId, DreamUpdateRequest request) {
        log.debug("Updating dream {} for user: {}", dreamId, user.getEmail());

        DreamEntry dream = dreamRepository.findByIdAndUserId(dreamId, user.getId())
            .orElseThrow(() -> new AccessDeniedException("Dream not found or access denied"));

        // Update all fields (PUT semantics)
        dream.setDate(request.date());
        dream.setTitle(request.title());
        dream.setContent(request.content());
        dream.setMoodInDream(request.moodInDream());
        dream.setMoodAfterDream(request.moodAfterDream());
        dream.setVividness(request.vividness() != null ? request.vividness() : 0);
        dream.setLucid(request.lucid() != null ? request.lucid() : false);
        // Wrap in ArrayList to ensure mutability for Hibernate
        dream.setTags(request.tags() != null ? new ArrayList<>(request.tags()) : new ArrayList<>());

        DreamEntry saved = dreamRepository.save(dream);
        log.info("Updated dream {} for user {}", dreamId, user.getEmail());
        return DreamResponse.from(saved);
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
}
