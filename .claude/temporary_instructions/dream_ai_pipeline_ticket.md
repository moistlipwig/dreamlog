# Dream Analysis & Image Generation Pipeline - Implementation Ticket

## ✅ User Decisions (Approved 2025-11-04)

1. **AI Provider:** Google AI Studio (Free tier)
   - Text analysis: **Gemini Flash** (latest version)
   - Image generation: **Imagen 3** (latest version) - "nano bana"
2. **Dependencies:** Use latest versions (db-scheduler 16.0.0, MinIO 8.6.0, Resilience4j 2.3.0)
3. **Database Cleanup:** ✅ Remove unused columns (`risk_score`, `recurring`, `language`, `style`)
4. **Rate Limiting:** **20 dreams/hour** per user (not 10)
5. **SSE Implementation:** ✅ Implement basic SSE on both backend + frontend
6. **Image Settings:**
   - Format: **JPEG** (not PNG)
   - Resolution: **Original from Imagen** (do not resize)
   - Pre-signed URL validity: **2 hours** (not 7 days)

**Status:** ✅ APPROVED - Proceeding to implementation

---

## 🎯 Progress Tracker

**Status:** 🟢 IMPLEMENTATION (Stage 1)

| Stage | Task | Status | Est. Time | Notes |
|-------|------|--------|-----------|-------|
| 0 | Pre-implementation Analysis | ✅ Complete | - | Architecture approved by user |
| 1 | Infrastructure Setup | 🔵 In Progress | 2h | MinIO, db-scheduler, dependencies |
| 2 | Database Schema & Migrations | ⚪ Pending | 1.5h | State tracking, image metadata, scheduler tables |
| 3 | Domain Events & Event Bus | ⚪ Pending | 1h | @TransactionalEventListener pattern |
| 4 | Ports & Adapters (Interfaces) | ⚪ Pending | 1.5h | AI service port, Storage port, abstractions |
| 5 | External Service Adapters | ⚪ Pending | 3h | Google AI Studio client, MinIO client, Resilience4j |
| 6 | Async Task Handlers (db-scheduler) | ⚪ Pending | 3h | AnalyzeTextTask, GenerateImageTask, retry logic |
| 7 | Domain Service Updates | ⚪ Pending | 2h | DreamService orchestration, state management |
| 8 | API Enhancements + SSE | ⚪ Pending | 2h | GET /dreams/{id} with analysis & image, SSE endpoint |
| 9 | Rate Limiting & Security | ⚪ Pending | 1.5h | User rate limiter (20/hour), content validation |
| 10 | Unit Tests | ⚪ Pending | 3h | Domain logic, task handlers, port mocks |
| 11 | Integration Tests | ⚪ Pending | 3h | Full flow test with mocked external services |
| 12 | Frontend SSE Integration | ⚪ Pending | 2h | Basic SSE client in Angular |
| 13 | Cleanup & Verification | ⚪ Pending | 1h | Code review, dead code removal |

**Total Estimated Time:** ~26 hours

---

## 📋 Goal

Implement an asynchronous AI pipeline that:
1. **Analyzes dream text** using Google AI Studio (LLM)
2. **Generates dream image** using Google AI Studio (image generation)
3. **Stores images** in MinIO (S3-compatible storage)
4. **Tracks processing state** with robust retry/failure handling
5. **Notifies frontend** via SSE/WebSocket when analysis is ready

### Why This Matters
- **Zero AI logic in HTTP transactions** - keeps API fast and responsive
- **Resilient async processing** - survives failures, retries automatically
- **Clean architecture** - swappable AI providers and storage backends
- **Scalable** - db-scheduler is cluster-safe for future horizontal scaling

---

## 🏗️ STAGE 0: Pre-implementation Analysis

### Current State Assessment

#### ✅ What Already Exists
1. **Entities:**
   - `DreamEntry` (backend/src/main/java/pl/kalin/dreamlog/dream/model/DreamEntry.java)
   - `DreamAnalysis` (backend/src/main/java/pl/kalin/dreamlog/dream/model/DreamAnalysis.java)
   - Tables: `dream_entry`, `dream_analysis`, `dream_analysis_tags`, `dream_analysis_entities`

2. **Service Layer:**
   - `DreamService` with CRUD operations
   - Authorization checks (user ownership)
   - Full-text search functionality

3. **API:**
   - POST /api/dreams (creates dream)
   - GET /api/dreams/{id}
   - Standard CRUD endpoints

4. **Infrastructure:**
   - PostgreSQL 18 in docker-compose
   - Spring Boot 3.5.5
   - Feature-based package structure (dream/*, user/*)

#### ❌ What's Missing
1. **Async Processing:** No event publishing, no task scheduling
2. **Object Storage:** No MinIO or S3 integration
3. **AI Integration:** No Google AI Studio client
4. **State Tracking:** No processing state enum in dream_entry
5. **Image Metadata:** No columns for image_uri, image storage info
6. **Retry/Resilience:** No retry logic or circuit breakers
7. **SSE/WebSocket:** No real-time notification mechanism

#### ⚠️ Issues to Clean Up (Boy Scout Rule)
1. **DreamResponse DTO** doesn't include:
   - Analysis data (currently requires separate fetch)
   - Image URI
   - Processing state
   - **Fix:** Enhance DreamResponse to include analysis summary + image info

2. **No validation** on dream content length:
   - LLM APIs have token limits (typically 32k-128k tokens)
   - **Fix:** Add @Size validation on content field (e.g., max 10,000 chars)

3. **Unused columns** in dream_analysis:
   - `risk_score`, `recurring`, `language`, `style` - are these needed?
   - **Decision:** Keep for future use OR remove if not planned

---

## 🎯 Architectural Design

### Decision: Ports & Adapters (Hexagonal Architecture)

**✅ YES - Use Ports & Adapters for:**

1. **AI Service Port** (`dream.ai.port.DreamAnalysisAiService`)
   - Interface defines domain needs: `analyzeText()`, `generateImage()`
   - Adapter: `GoogleAiStudioAdapter` (backend/src/main/java/pl/kalin/dreamlog/dream/ai/adapter/)
   - **Why:** Allows swapping AI providers (OpenAI, Claude, local models) without touching domain
   - **Benefit:** Easy testing with mocks, no vendor lock-in

2. **Image Storage Port** (`dream.storage.port.ImageStorageService`)
   - Interface: `store()`, `getPresignedUrl()`, `delete()`
   - Adapter: `MinioImageStorageAdapter`
   - **Why:** Abstracts S3/MinIO/filesystem, keeps domain storage-agnostic
   - **Benefit:** Can switch to AWS S3, Azure Blob, or local filesystem in tests

3. **Event Publisher Port** (Use Spring's `ApplicationEventPublisher`)
   - **Why:** Spring provides this out-of-box, no need for custom port
   - **Benefit:** Standard Spring mechanism, @TransactionalEventListener support

**❌ NO - Keep Simple for:**
- Repository layer (already using Spring Data JPA)
- HTTP controllers (standard Spring MVC)
- Configuration (Spring Boot auto-configuration)

### Domain Events Strategy

```
HTTP POST /dreams
    ↓
DreamService.createDream()
    ↓ saves DreamEntry with state=CREATED
    ↓ @Transactional
    ↓
@TransactionalEventListener (AFTER_COMMIT)
    ↓
Publishes DreamCreatedEvent
    ↓
db-scheduler: AnalyzeTextTask scheduled
```

**Event Flow:**
1. `DreamCreatedEvent` → schedules `AnalyzeTextTask`
2. `TextAnalysisCompletedEvent` → schedules `GenerateImageTask`
3. `ImageGenerationCompletedEvent` → sets state=COMPLETED, sends SSE
4. `AnalysisFailedEvent` → sets state=FAILED, logs error

### State Machine

```java
public enum DreamProcessingState {
    CREATED,              // Initial state after POST /dreams
    ANALYZING_TEXT,       // Task A in progress
    TEXT_ANALYZED,        // Task A completed, before Task B
    GENERATING_IMAGE,     // Task B in progress
    COMPLETED,            // All done, analysis + image ready
    FAILED                // Unrecoverable failure after max retries
}
```

**Transitions:**
- CREATED → ANALYZING_TEXT (when Task A starts)
- ANALYZING_TEXT → TEXT_ANALYZED (when analysis saved)
- TEXT_ANALYZED → GENERATING_IMAGE (when Task B starts)
- GENERATING_IMAGE → COMPLETED (when image saved)
- Any state → FAILED (after 8 failed retries)

### Task Scheduling with db-scheduler

**Why db-scheduler over @Scheduled or Spring Task Executor?**
- ✅ **Persistent:** Tasks survive app restarts
- ✅ **Cluster-safe:** Multiple instances coordinate via DB
- ✅ **Built-in retry:** Exponential backoff out-of-box
- ✅ **Monitoring:** Query `scheduled_tasks` table for status
- ✅ **No message broker:** Simpler than Kafka/RabbitMQ for this use case

**Task Definitions:**

1. **AnalyzeTextTask** (recurring, immediate execution)
   - Input: dreamId
   - Logic:
     ```java
     1. Load dream entry
     2. Check if analysis already exists → skip (idempotency)
     3. Set state = ANALYZING_TEXT
     4. Call aiService.analyzeText(dream.content)
     5. Save DreamAnalysis entity
     6. Set state = TEXT_ANALYZED
     7. Schedule GenerateImageTask
     ```
   - Retry: 15min intervals, max 8 attempts

2. **GenerateImageTask** (one-time, scheduled by AnalyzeTextTask)
   - Input: dreamId
   - Logic:
     ```java
     1. Load dream entry + analysis
     2. Check if image already exists → skip (idempotency)
     3. Set state = GENERATING_IMAGE
     4. Call aiService.generateImage(analysis.summary)
     5. Upload image to storage: storageService.store(imageBytes)
     6. Save image URI to dream_entry
     7. Set state = COMPLETED
     8. Publish ImageGenerationCompletedEvent (for SSE)
     ```
   - Retry: 15min intervals, max 8 attempts

### Retry & Resilience Strategy

**Two-Layer Approach:**

1. **db-scheduler Retry** (task-level):
   - Handles transient failures (network errors, rate limits)
   - Exponential backoff: 15min, 30min, 1h, 2h, 4h, 8h, 16h, 32h (8 attempts)
   - After 8 failures → set state=FAILED, stop retrying

2. **Resilience4j on RestTemplate** (call-level):
   - Circuit breaker: Open after 5 consecutive failures, half-open after 1min
   - Retry: 3 attempts with 2s, 4s, 8s backoff (separate from db-scheduler)
   - Rate limiter: Max 10 req/sec per AI endpoint
   - **Why:** Fast-fail for immediate errors, db-scheduler handles longer-term retries

---

## 📊 Database Schema Changes

### Migration V5: Add Processing State & Image Metadata

```sql
-- Add processing state enum
CREATE TYPE dream_processing_state AS ENUM (
    'CREATED',
    'ANALYZING_TEXT',
    'TEXT_ANALYZED',
    'GENERATING_IMAGE',
    'COMPLETED',
    'FAILED'
);

-- Add columns to dream_entry
ALTER TABLE dream_entry
    ADD COLUMN processing_state dream_processing_state NOT NULL DEFAULT 'CREATED',
    ADD COLUMN image_uri TEXT,                    -- S3/MinIO URI
    ADD COLUMN image_storage_key VARCHAR(255),    -- Object key in bucket
    ADD COLUMN image_generated_at TIMESTAMPTZ,
    ADD COLUMN failure_reason TEXT,               -- Error message if FAILED
    ADD COLUMN retry_count INT DEFAULT 0;

-- Index for querying processing state
CREATE INDEX idx_dream_entry_processing_state ON dream_entry(processing_state);
```

### Migration V6: db-scheduler Tables

```sql
-- Standard db-scheduler schema (from official documentation)
CREATE TABLE scheduled_tasks (
    task_name TEXT NOT NULL,
    task_instance TEXT NOT NULL,
    task_data BYTEA,
    execution_time TIMESTAMPTZ NOT NULL,
    picked BOOLEAN NOT NULL,
    picked_by TEXT,
    last_success TIMESTAMPTZ,
    last_failure TIMESTAMPTZ,
    consecutive_failures INT,
    last_heartbeat TIMESTAMPTZ,
    version BIGINT NOT NULL,
    PRIMARY KEY (task_name, task_instance)
);

CREATE INDEX idx_scheduled_tasks_execution_time ON scheduled_tasks(execution_time);
CREATE INDEX idx_scheduled_tasks_picked ON scheduled_tasks(picked);
```

### Migration V7: Update dream_analysis (optional cleanup)

**Decision Needed:** Do we need these columns?
- `risk_score` (mental health risk detection?)
- `recurring` (recurring dream flag?)
- `language` (detected language?)
- `style` (dream narrative style?)

**Recommendation:**
- If not in scope for Phase 4, remove them (YAGNI principle)
- If planned, add comments explaining their purpose

---

## 🎨 LLM Prompts Design

### Text Analysis Prompt

```
You are an expert dream analyst specializing in symbolism, emotions, and psychological interpretation.

**Dream Description:**
{dream.content}

**Task:** Analyze this dream and return a JSON response with the following structure:

{
  "summary": "Brief 1-2 sentence summary of the dream",
  "tags": ["tag1", "tag2", "tag3"],  // Max 10 thematic tags (e.g., "flying", "water", "fear")
  "entities": ["entity1", "entity2"],  // Key people, places, objects mentioned
  "emotions": {
    "joy": 0.7,
    "fear": 0.3,
    "anger": 0.1
  },  // Emotional intensity scores (0.0-1.0)
  "interpretation": "Detailed psychological interpretation focusing on symbolism and meaning",
  "recurring": true/false  // Likely to be a recurring dream theme?
}

**Guidelines:**
- Keep summary concise (max 100 words)
- Tags should be lowercase, single words or short phrases
- Emotions should sum to ~1.0
- Interpretation should be insightful but not prescriptive
```

### Image Generation Prompt

```
Create a dreamlike, surreal image based on this dream summary:

**Dream Summary:**
{analysis.summary}

**Style:** Dreamlike, ethereal, slightly surreal but not nightmare-inducing. Use soft lighting and dream-like color palettes (pastels, deep blues, purples). Should evoke the emotional tone of the dream.

**Key Elements to Include:** {analysis.entities[0]}, {analysis.entities[1]}, {analysis.entities[2]}

**Mood:** {primaryEmotion from analysis.emotions}

**Format:** 1024x1024, digital art style
```

---

## 📦 New Dependencies (build.gradle)

```gradle
dependencies {
    // Existing dependencies...

    // db-scheduler for persistent task scheduling
    implementation 'com.github.kagkarlsson:db-scheduler-spring-boot-starter:14.0.3'

    // MinIO client (S3-compatible)
    implementation 'io.minio:minio:8.5.7'

    // Resilience4j for circuit breaker, retry, rate limiter
    implementation 'org.springframework.boot:spring-boot-starter-aop'
    implementation 'io.github.resilience4j:resilience4j-spring-boot3:2.2.0'
    implementation 'io.github.resilience4j:resilience4j-circuitbreaker:2.2.0'
    implementation 'io.github.resilience4j:resilience4j-ratelimiter:2.2.0'
    implementation 'io.github.resilience4j:resilience4j-retry:2.2.0'

    // HTTP client for AI API calls (or keep existing RestTemplate)
    implementation 'org.springframework.boot:spring-boot-starter-web'  // Already included

    // SSE support (already in spring-boot-starter-web)

    // For testing: Testcontainers for MinIO (optional, can use mocks)
    testImplementation 'org.testcontainers:testcontainers:1.19.3'
}
```

---

## 🐳 Docker Compose Updates

```yaml
services:
  # Existing db service...

  minio:
    image: minio/minio:latest
    command: server /data --console-address ":9001"
    environment:
      MINIO_ROOT_USER: ${MINIO_ACCESS_KEY:-minioadmin}
      MINIO_ROOT_PASSWORD: ${MINIO_SECRET_KEY:-minioadmin}
    ports:
      - "9000:9000"      # S3 API
      - "9001:9001"      # Web UI
    volumes:
      - minio_data:/data
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:9000/minio/health/live"]
      interval: 10s
      timeout: 5s
      retries: 5

volumes:
  dbdata:
  minio_data:  # New volume
```

---

## 🧪 Testing Strategy

### Unit Tests (Fast, No Spring Context)

1. **Task Handlers (Spock):**
   - `AnalyzeTextTaskSpec`: Mock AI port, verify state transitions
   - `GenerateImageTaskSpec`: Mock storage port, verify idempotency

2. **Domain Logic:**
   - State machine transitions
   - Validation rules (content length, rate limits)

### Integration Tests (Spring Context + Testcontainers)

**CRITICAL: Use MOCKS for External Services!**

```groovy
@SpringBootTest
@Testcontainers
class DreamAiPipelineIntegrationSpec extends IntegrationSpec {

    @MockBean
    DreamAnalysisAiService aiService  // Mock Google AI

    @MockBean
    ImageStorageService storageService  // Mock MinIO

    @Autowired
    DreamService dreamService

    def "should complete full AI pipeline from dream creation to image generation"() {
        given: "user creates a dream"
        def user = createTestUser()
        def createRequest = new DreamCreateRequest(
            date: LocalDate.now(),
            content: "I was flying over mountains...",
            ...
        )

        and: "AI service returns mock analysis"
        aiService.analyzeText(_) >> new AnalysisResult(
            summary: "Dream about freedom and exploration",
            tags: ["flying", "mountains"],
            emotions: [joy: 0.8, fear: 0.2]
        )

        and: "storage service returns mock URI"
        storageService.store(_, _) >> "s3://dreams/images/test-dream.png"

        when: "dream is created"
        def dreamId = dreamService.createDream(user, createRequest)

        and: "wait for async tasks to complete (or trigger manually in test)"
        // Trigger task execution manually or use Awaitility
        taskScheduler.runNow("analyze-text", dreamId)
        taskScheduler.runNow("generate-image", dreamId)

        then: "dream is in COMPLETED state with analysis and image"
        def dream = dreamRepository.findById(dreamId).get()
        dream.processingState == DreamProcessingState.COMPLETED
        dream.imageUri != null

        and: "analysis was saved"
        def analysis = dreamAnalysisRepository.findByDreamId(dreamId).get()
        analysis.summary == "Dream about freedom and exploration"
        analysis.tags.contains("flying")
    }

    def "should handle AI service failure with retry and eventual FAILED state"() {
        given: "AI service fails"
        aiService.analyzeText(_) >> { throw new AiServiceException("Rate limit exceeded") }

        when: "dream is created and task retries 8 times"
        def dreamId = dreamService.createDream(user, createRequest)
        8.times { taskScheduler.runNow("analyze-text", dreamId) }

        then: "dream is marked as FAILED"
        def dream = dreamRepository.findById(dreamId).get()
        dream.processingState == DreamProcessingState.FAILED
        dream.failureReason.contains("Rate limit exceeded")
    }
}
```

### What NOT to Test (Save Time!)
- ❌ Google AI Studio API integration (assume it works, mock it)
- ❌ MinIO upload/download (assume it works, mock it)
- ❌ Load testing (out of scope for Phase 4)
- ❌ Browser SSE testing (backend SSE endpoint only)

---

## 🔒 Rate Limiting & Security

### User Rate Limiter

**Requirement:** Prevent abuse of AI generation (expensive API calls)

**Implementation:**
```java
@Service
public class DreamCreationRateLimiter {
    private final Map<UUID, RateLimiter> userLimiters = new ConcurrentHashMap<>();

    public boolean allowCreate(User user) {
        RateLimiter limiter = userLimiters.computeIfAbsent(
            user.getId(),
            id -> RateLimiter.of("user-" + id, RateLimiterConfig.custom()
                .limitForPeriod(10)           // 10 dreams
                .limitRefreshPeriod(Duration.ofHours(1))  // per hour
                .timeoutDuration(Duration.ZERO)
                .build())
        );
        return limiter.acquirePermission();
    }
}
```

**Usage in DreamController:**
```java
@PostMapping
public ResponseEntity<CreatedResponse> createDream(...) {
    if (!rateLimiter.allowCreate(user)) {
        return ResponseEntity.status(429).build();  // Too Many Requests
    }
    // ... proceed with creation
}
```

### Content Validation

```java
public record DreamCreateRequest(
    @NotNull LocalDate date,
    @NotBlank String content,
    @Size(max = 10000, message = "Dream content too long (max 10,000 chars)")
    String content,
    ...
) {}
```

---

## 🎯 API Changes

### Enhanced GET /dreams/{id}

**Current Response:**
```json
{
  "id": "uuid",
  "date": "2025-01-15",
  "title": "Flying Dream",
  "content": "I was flying...",
  "tags": ["flying"]
}
```

**New Response (with analysis + image):**
```json
{
  "id": "uuid",
  "date": "2025-01-15",
  "title": "Flying Dream",
  "content": "I was flying...",
  "tags": ["flying"],
  "processingState": "COMPLETED",
  "analysis": {
    "summary": "Dream about freedom",
    "tags": ["flying", "freedom"],
    "emotions": {"joy": 0.8},
    "interpretation": "This dream suggests..."
  },
  "image": {
    "uri": "https://minio.local:9000/dreams/abc123.png",
    "generatedAt": "2025-01-15T10:30:00Z"
  }
}
```

### New SSE Endpoint: GET /api/dreams/{id}/progress

**Purpose:** Real-time updates for frontend

```java
@GetMapping(value = "/{id}/progress", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public Flux<ServerSentEvent<DreamProgressUpdate>> streamProgress(
    @PathVariable UUID id,
    Authentication auth
) {
    // Implementation: Subscribe to processing events for this dream
}
```

**Event Format:**
```json
{
  "dreamId": "uuid",
  "state": "ANALYZING_TEXT",
  "progress": 50,
  "message": "Analyzing dream content..."
}
```

---

## 📝 Implementation Stages (Detailed Breakdown)

### STAGE 1: Infrastructure Setup (2h)

**Tasks:**
- [x] Add dependencies to build.gradle (db-scheduler, MinIO, Resilience4j)
- [x] Add MinIO service to docker-compose.yml
- [x] Create application.yml configuration for:
  - Google AI API key/secret (from .env)
  - MinIO connection details
  - db-scheduler settings
  - Resilience4j circuit breaker config
- [x] Create MinIO bucket on startup (via @PostConstruct or config)
- [x] Test: Start docker-compose, verify MinIO UI at http://localhost:9001

**Deliverables:**
- Updated build.gradle
- Updated docker-compose.yml
- application.yml with new configs
- README section on setting GOOGLE_AI_API_KEY

---

### STAGE 2: Database Schema & Migrations (1.5h)

**Tasks:**
- [x] Create V5__add_processing_state_and_image_metadata.sql
- [x] Create V6__create_db_scheduler_tables.sql
- [x] (Optional) Create V7__cleanup_unused_dream_analysis_columns.sql
- [x] Update DreamEntry entity with new fields
- [x] Test: Run ./gradlew :backend:bootRun, verify migrations applied

**Deliverables:**
- 2-3 new Flyway migrations
- Updated DreamEntry.java
- Database reflects new schema

---

### STAGE 3: Domain Events & Event Bus (1h)

**Tasks:**
- [x] Create event POJOs:
  - `DreamCreatedEvent`
  - `TextAnalysisCompletedEvent`
  - `ImageGenerationCompletedEvent`
  - `AnalysisFailedEvent`
- [x] Create `DreamEventListener` with @TransactionalEventListener methods
- [x] Update DreamService.createDream() to publish DreamCreatedEvent
- [x] Test: Log event firing, verify AFTER_COMMIT timing

**Deliverables:**
- 4 event classes in `dream.events/`
- DreamEventListener.java
- Logging shows events firing

---

### STAGE 4: Ports & Adapters (Interfaces) (1.5h)

**Tasks:**
- [x] Create port interfaces:
  ```
  dream/ai/port/DreamAnalysisAiService.java
  dream/storage/port/ImageStorageService.java
  ```
- [x] Define method signatures:
  ```java
  // DreamAnalysisAiService
  AnalysisResult analyzeText(String content);
  ImageGenerationResult generateImage(String prompt);

  // ImageStorageService
  String store(byte[] imageData, String filename);
  String getPresignedUrl(String storageKey);
  void delete(String storageKey);
  ```
- [x] Create DTOs: `AnalysisResult`, `ImageGenerationResult`

**Deliverables:**
- 2 port interfaces
- DTOs for AI results
- Clear separation: domain uses ports, not implementations

---

### STAGE 5: External Service Adapters (3h)

**Tasks:**
- [x] Implement `GoogleAiStudioAdapter implements DreamAnalysisAiService`
  - RestTemplate with Resilience4j decorators (circuit breaker, retry, rate limiter)
  - POST to Google AI Studio API (Gemini for text, Imagen for image)
  - Parse JSON responses
  - Handle errors (rate limits, timeouts)
- [x] Implement `MinioImageStorageAdapter implements ImageStorageService`
  - MinioClient bean configuration
  - store() method: upload to "dreamlog-images" bucket
  - getPresignedUrl() method: generate temporary signed URL
  - delete() method: remove object
- [x] Configure Resilience4j in application.yml
- [x] Test: Unit tests with WireMock for AI API, real MinIO in Testcontainers

**Deliverables:**
- GoogleAiStudioAdapter.java
- MinioImageStorageAdapter.java
- Resilience4j config
- Adapter tests

---

### STAGE 6: Async Task Handlers (db-scheduler) (3h)

**Tasks:**
- [x] Configure db-scheduler in application.yml
- [x] Create `AnalyzeTextTask implements OneTimeTask<DreamTaskData>`
  - Load dream by ID
  - Check if analysis exists (idempotency)
  - Update state → ANALYZING_TEXT
  - Call aiService.analyzeText()
  - Save DreamAnalysis entity
  - Update state → TEXT_ANALYZED
  - Schedule GenerateImageTask
  - Handle failures: log, increment retry_count, set FAILED after 8 attempts
- [x] Create `GenerateImageTask implements OneTimeTask<DreamTaskData>`
  - Load dream + analysis
  - Check if image exists (idempotency)
  - Update state → GENERATING_IMAGE
  - Call aiService.generateImage()
  - Upload image via storageService.store()
  - Save image_uri to dream_entry
  - Update state → COMPLETED
  - Publish ImageGenerationCompletedEvent
- [x] Register tasks with Scheduler bean
- [x] Test: Manual task execution in integration test

**Deliverables:**
- AnalyzeTextTask.java
- GenerateImageTask.java
- TaskSchedulerConfig.java
- Task execution tests

---

### STAGE 7: Domain Service Updates (2h)

**Tasks:**
- [x] Update DreamService.createDream():
  - Set initial state = CREATED
  - Validate content length
  - After save, publish DreamCreatedEvent
- [x] Add DreamService.getDreamWithAnalysis(UUID dreamId):
  - Fetch dream + analysis + image in single query (or lazy load)
  - Return enhanced DreamResponse
- [x] Add state transition methods (optional):
  - `transitionToAnalyzing()`, `transitionToCompleted()`, etc.
  - Or keep state updates in task handlers (simpler)

**Deliverables:**
- Updated DreamService.java
- State transition logic encapsulated
- Service tests

---

### STAGE 8: API Enhancements (1.5h)

**Tasks:**
- [x] Update DreamResponse DTO:
  - Add `processingState` field
  - Add nested `AnalysisResponse` (summary, tags, emotions)
  - Add nested `ImageResponse` (uri, generatedAt)
- [x] Update DreamController.getDreamById():
  - Return enhanced DreamResponse with analysis + image
- [x] Add SSE endpoint: `/api/dreams/{id}/progress`
  - Subscribe to ImageGenerationCompletedEvent
  - Stream progress updates (optional for Phase 1, can be future work)
- [x] Test: Postman/curl verify new response structure

**Deliverables:**
- Updated DreamResponse.java
- Enhanced GET /dreams/{id}
- (Optional) SSE endpoint
- API tests

---

### STAGE 9: Rate Limiting & Security (1.5h)

**Tasks:**
- [x] Implement DreamCreationRateLimiter service
- [x] Add rate limiting check in DreamController.createDream()
- [x] Return 429 Too Many Requests if limit exceeded
- [x] Add @Size(max = 10000) to DreamCreateRequest.content
- [x] Test: Verify rate limiting works (create 11 dreams in 1 hour)

**Deliverables:**
- DreamCreationRateLimiter.java
- Updated DreamController
- Validation tests

---

### STAGE 10: Unit Tests (3h)

**Tasks:**
- [x] Write Spock specs for:
  - Task handlers (AnalyzeTextTaskSpec, GenerateImageTaskSpec)
  - Event listeners (DreamEventListenerSpec)
  - Rate limiter (DreamCreationRateLimiterSpec)
  - Domain logic (state transitions)
- [x] Use mocks for ports (aiService, storageService)
- [x] Test edge cases:
  - Idempotency (task runs twice, no duplicate work)
  - Failures (AI returns error, task fails gracefully)
  - Rate limit exceeded

**Deliverables:**
- 10+ unit test specs
- 90%+ code coverage for new code
- All tests green

---

### STAGE 11: Integration Tests (3h)

**Tasks:**
- [x] Write DreamAiPipelineIntegrationSpec:
  - Full flow: create dream → analyze text → generate image → COMPLETED
  - Failure flow: AI fails → retry 8 times → FAILED state
  - Idempotency: task runs twice, no duplicate analysis/image
- [x] Use @MockBean for aiService and storageService
- [x] Use real db-scheduler with Testcontainers Postgres
- [x] Use Awaitility or manual task triggering for async verification
- [x] Test: Run ./gradlew :backend:test --tests "*IntegrationSpec"

**Deliverables:**
- DreamAiPipelineIntegrationSpec.groovy
- Integration test covers full happy path + failure scenarios
- Tests pass reliably

---

### STAGE 12: Cleanup & Refactoring (1h)

**Tasks:**
- [x] Boy Scout Rule:
  - Remove unused imports
  - Extract magic numbers to constants
  - Add JavaDocs to public methods
  - Rename unclear variables
- [x] Check for code duplication (DRY principle)
- [x] Review logs: ensure no sensitive data logged (API keys, user content)
- [x] Update README:
  - Add section on AI pipeline architecture
  - Document environment variables (GOOGLE_AI_API_KEY, etc.)
  - Add troubleshooting section
- [x] Final verification: ./gradlew :backend:build

**Deliverables:**
- Clean, documented code
- Updated README
- All tests pass
- Ready for user review

---

## ✅ Definition of Done

**Task is complete when:**
1. ✅ User can POST /dreams and see state=CREATED immediately
2. ✅ Async pipeline analyzes text and generates image without blocking HTTP request
3. ✅ GET /dreams/{id} returns full dream with analysis, image URI, and processing state
4. ✅ Failed AI calls retry up to 8 times with exponential backoff
5. ✅ Rate limiter prevents >10 dream creations per user per hour
6. ✅ All unit tests pass (90%+ coverage)
7. ✅ Integration test covers full flow with mocked external services
8. ✅ Code follows clean code principles (DRY, SOLID, KISS)
9. ✅ README documents new environment variables and architecture
10. ✅ Docker compose up works with MinIO and db-scheduler

---

## 🤔 Open Questions for User

1. **Google AI Studio API:**
   - Do you already have an API key?
   - Which models to use? (Gemini 1.5 Pro for text, Imagen 2 for images?)
   - Any rate limits or budget constraints?

2. **Image Storage:**
   - Bucket name: "dreamlog-images" OK?
   - Pre-signed URL expiry: 7 days?
   - Image format: PNG or JPEG? Resolution: 1024x1024?

3. **Analysis Schema:**
   - Are `risk_score`, `recurring`, `language`, `style` fields needed in dream_analysis?
   - Or should we remove them (YAGNI)?

4. **SSE/WebSocket:**
   - Phase 4 priority or defer to Phase 5?
   - If deferred, frontend polls GET /dreams/{id} for state updates

5. **Rate Limiting:**
   - 10 dreams/hour per user OK?
   - Or different limits (e.g., 50/day)?

---

## 📚 References

- [db-scheduler Documentation](https://github.com/kagkarlsson/db-scheduler)
- [Resilience4j Spring Boot Guide](https://resilience4j.readme.io/docs/getting-started-3)
- [MinIO Java Client](https://min.io/docs/minio/linux/developers/java/minio-java.html)
- [Spring @TransactionalEventListener](https://docs.spring.io/spring-framework/reference/data-access/transaction/event.html)
- [Hexagonal Architecture (Ports & Adapters)](https://alistair.cockburn.us/hexagonal-architecture/)

---

**Status:** 🔵 Awaiting user approval to proceed with implementation

**Last Updated:** 2025-11-04
**Author:** Claude (AI Assistant)
**Review:** Ready for user review and Stage 1 kickoff
