# 🌙 Dziennik Snów — README

## 🎯 Cel projektu

Aplikacja do rejestrowania i analizowania snów z naciskiem na naukę realnych technologii:
**Java 21 (Spring Boot), Angular, OIDC, PostgreSQL (FTS + trigramy), PWA, SSE, gRPC, observability, AI-service.**  
Projekt ma charakter edukacyjno‑rozrywkowy, ale z potencjałem do komercjalizacji.

---

## 🛠️ Stos technologiczny

### Backend

- Java **21** + Spring Boot **3.3+**
- Spring Security (OIDC Client + Resource Server)
- Spring Web (REST, SSE), gRPC (między usługami)
- Postgres 15/16 + Flyway, JPA/Hibernate, HikariCP
- Micrometer + Prometheus + Grafana, OpenTelemetry
- Testcontainers (Postgres, gRPC)

### Frontend

- Angular 20+ (Standalone Components in catalogue ./frontend)
- Angular Material + TailwindCSS
- ngx‑charts / ng2‑charts
- PWA (offline, outbox), SSE wrapper
- State management: Angular serwisy → opcjonalnie @ngrx/component-store
- Unit tests: Jest + jest-preset-angular

### Infrastruktura

- Docker Compose: postgres, backend, ai-service, minio, prometheus, grafana
- CI/CD: GitHub Actions (build, test, lint)
- Secrets: `.env` lokalnie, Vault w późniejszej fazie

---

## 🔑 Autoryzacja (OIDC / OAuth2)

- **IdP: Google OAuth** (Authorization Code + PKCE)
- Backend trzyma tokeny → sesja w cookie (BFF pattern)
- Angular korzysta z backendu poprzez cookie HttpOnly (brak tokenów w localStorage)
- Logout: RP-initiated logout z Google

---

## 📚 Roadmapa / Kroki realizacji

Każda faza zawiera **cel**, **zakres** oraz **kryteria ukończenia (DoD)**, które jednoznacznie pozwalają przejść do kolejnej.

### Faza 0 — Setup (in progress)

**Cel:** Postawić środowisko deweloperskie i CI.

- Repozytorium, CI/CD (build+test), Docker Compose (Postgres), Spring Boot skeleton (Actuator, Swagger), Angular skeleton (Material+Tailwind).  
  **DoD:** `docker compose up` podnosi bazę; CI testy przechodzą; backend `/actuator/health` = UP; frontend działa lokalnie.

**Co się nauczę:**

- Konfiguracja środowiska deweloperskiego i narzędzi CI/CD
- Podstawy Docker Compose i integracja z bazą danych
- Tworzenie szkieletu aplikacji backend i frontend z wykorzystaniem Spring Boot i Angular

**Pytania do zadania:**

- Jak skonfigurować Docker Compose, aby uruchomić wszystkie usługi lokalnie?
- W jaki sposób Actuator i Swagger pomagają w rozwoju i testowaniu backendu?
- Jak zapewnić, że CI poprawnie buduje i testuje projekt?

### Faza 1 — Auth (Multi-provider + BFF) - in progress

**Cel:** Logowanie przez Google/Facebook OAuth oraz manual registration w modelu BFF.

- Konfiguracja Spring Security + OAuth2 (Google, Facebook), endpoint `/api/me`, `/api/auth/*`
- Manual registration
- Account linking (OAuth ↔ local credentials)
- Angular guardy, login/register components, logout
- Session-based auth z HttpOnly cookies, CSRF protection
  **DoD:**
  - User może zarejestrować się przez email/password, Google lub Facebook
  - User może połączyć konto OAuth z local credentials i odwrotnie
  - Zalogowany użytkownik ma cookie HttpOnly; `/api/me` zwraca user info
  - Logout działa poprawnie; testy integracyjne pokrywają wszystkie flow

**Co się nauczę:**

- Implementacja OAuth2 Authorization Code Flow z PKCE w Spring Security
- Mechanizm BFF (Backend For Frontend) i bezpieczne przechowywanie sesji w HttpOnly cookies
- Multi-provider authentication (Google, Facebook, local)
- Account linking patterns i zarządzanie federated identities
- Obsługa autoryzacji i uwierzytelniania po stronie frontendu i backendu

**Pytania do zadania:**

- Jak działa Authorization Code Flow z PKCE i dlaczego jest bezpieczniejszy?
- Jak zabezpieczyć sesję użytkownika, aby tokeny nie były dostępne w JavaScript?
- W jaki sposób Angular guardy współpracują z backendem w modelu BFF?
  - Jak bezpiecznie przechowywać hasła (Argon2id vs BCrypt)?
- Jak zaimplementować account linking bez security vulnerabilities?

**Deferred to Phase 6 (Notifications):**

- Email verification for manual registration
- Password reset flow (forgot password)

### Faza 2 — CRUD snów + FTS/trigramy

**Cel:** Zapis i wyszukiwanie snów.

- Model `DreamEntry`, Flyway migracje (`unaccent`, `pg_trgm`), CRUD + `/search`, Angular lista i formularz.  
  **DoD:** CRUD działa end‑to‑end; wyszukiwanie zwraca poprawne wyniki <200 ms na 1000 seedów.

**Co się nauczę:**

- Tworzenie pełnotekstowego wyszukiwania (FTS) w PostgreSQL z wykorzystaniem unaccent i trigramów
- Projektowanie migracji bazy danych przy pomocy Flyway
- Budowa REST API CRUD i integracja z frontendem Angular

**Pytania do zadania:**

- Jak działa pełnotekstowe wyszukiwanie w PostgreSQL i kiedy używać trigramów?
- Jak zapewnić aktualizację indeksów FTS przy zmianie danych?
- W jaki sposób zoptymalizować zapytania wyszukiwania pod kątem wydajności?

### Faza 3 — Nastrój i statystyki

**Cel:** Rejestrowanie nastrojów i proste statystyki.

- Model `MoodEntry`, API korelacji snów i nastrojów, Angular dashboard z wykresem.  
  **DoD:** Dashboard wyświetla trend; testy korelacji przechodzą.

**Co się nauczę:**

- Modelowanie relacji między różnymi typami danych w bazie (sny i nastroje)
- Tworzenie API do analizy i korelacji danych
- Wizualizacja danych i tworzenie dashboardów w Angular z wykresami

**Pytania do zadania:**

- Jak zaprojektować API, które agreguje i analizuje dane z różnych źródeł?
- Jakie biblioteki Angular najlepiej nadają się do wizualizacji danych?
- W jaki sposób testować poprawność i wydajność zapytań analitycznych?

### Faza 4 — AI‑service + SSE

**Cel:** Asynchroniczna analiza snów.

- gRPC kontrakt, ai-service (mock → LLM), event SSE `ANALYSIS_READY`, Angular obsługa statusów.  
  **DoD:** „Analizuj” uruchamia analizę; wynik przychodzi przez SSE; test kontraktowy gRPC przechodzi.

**Co się nauczę:**

- Definiowanie i implementacja kontraktów gRPC między usługami
- Integracja asynchronicznych powiadomień przez Server-Sent Events (SSE)
- Podstawy integracji z usługami AI i obsługa statusów w UI

**Pytania do zadania:**

- Jak zaprojektować i przetestować kontrakt gRPC między mikroserwisami?
- W jaki sposób SSE różni się od WebSocket i kiedy go używać?
- Jak bezpiecznie i efektywnie obsługiwać długotrwałe zadania asynchroniczne?

### **Faza 4.1 — Wyszukiwanie semantyczne (pgvector + Spring AI)**

- Docker Compose: Postgres z rozszerzeniem `pgvector`
- Encja `DreamEmbedding` (powiązana z `DreamEntry`)
- Generowanie embeddingów (np. OpenAI, Ollama, HuggingFace)
- Endpoint: znajdź sny podobne do X (`ORDER BY embedding <-> :vec LIMIT n`)
- Spring AI: integracja klienta LLM + repozytorium wektorowe

**Co się nauczę:**

- Wykorzystanie rozszerzenia pgvector w PostgreSQL do przechowywania i wyszukiwania wektorów
- Generowanie i wykorzystanie embeddingów tekstowych z modeli LLM
- Integracja Spring AI z repozytorium wektorowym i zapytania semantyczne

**Pytania do zadania:**

- Jak działa wyszukiwanie najbliższych sąsiadów (nearest neighbor) w bazie danych?
- Jak przygotować i przechowywać embeddingi dla danych tekstowych?
- Jak integrować modele LLM z aplikacjami backendowymi?

### Faza 5 — PWA + Offline + Outbox

**Cel:** Działanie offline z późniejszą synchronizacją.

- Service Worker, IndexedDB, outbox dla mutacji, UI status offline/sync.  
  **DoD:** Tryb samolotowy: zapisany sen widoczny offline; po powrocie sieci synchronizacja działa; test e2e przechodzi.

**Co się nauczę:**

- Tworzenie Progressive Web App z obsługą trybu offline i Service Workerami
- Przechowywanie danych lokalnie za pomocą IndexedDB
- Wzorzec outbox do synchronizacji mutacji po przywróceniu połączenia

**Pytania do zadania:**

- Jak zaprojektować Service Workera, aby obsługiwał cache i synchronizację danych?
- W jaki sposób IndexedDB różni się od localStorage i kiedy go używać?
- Jak zapewnić spójność danych podczas pracy offline i synchronizacji?

### Faza 6 — Powiadomienia + Email Features

**Cel:** Przypomnienia o zapisie snów + email verification + password reset.

- Spring Scheduler (21:00), SSE `REMINDER`, Angular notyfikacja
- Email service (SMTP) dla verification i password reset
- Email verification flow (verification tokens, expiry)
- Password reset flow (reset tokens, secure link generation)
  **DoD:**
  - O 21:00 w UI pojawia się przypomnienie; można je wyłączyć w ustawieniach
  - Email verification działa dla manual registration
  - Password reset link wysyłany na email; link wygasa po 24h
  - Testy integracyjne pokrywają email flows

**Co się nauczę:**

- Konfiguracja harmonogramu zadań w Spring Scheduler
- Wysyłanie powiadomień w czasie rzeczywistym przez SSE
- Obsługa i zarządzanie powiadomieniami po stronie frontendowej
- Email service integration (Spring Mail, Mailhog dla dev)
- Secure token generation i expiry mechanisms
- Password reset security best practices

**Pytania do zadania:**

- Jak działa Spring Scheduler i jakie są jego możliwości?
- W jaki sposób implementować powiadomienia push w aplikacji webowej?
- Jak zapewnić możliwość wyłączania powiadomień przez użytkownika?
- Jak bezpiecznie implementować password reset tokens?
- Jak zapobiec token reuse i timing attacks?

### Faza 7 — Observability i prod‑ready

**Cel:** Monitoring i bezpieczeństwo.

- Prometheus + Grafana, OpenTelemetry, rate limiting, Idempotency‑Key, RODO endpoints.  
  **DoD:** Dashboard z metrykami działa; limiter zwraca 429 po progu; eksport/usuwanie konta działa.

**Co się nauczę:**

- Konfiguracja monitoringu aplikacji z Prometheus i Grafana
- Implementacja distributed tracing i metryk z OpenTelemetry
- Mechanizmy rate limiting i idempotency w API oraz zgodność z RODO

**Pytania do zadania:**

- Jak zaprojektować i wdrożyć efektywny monitoring aplikacji?
- W jaki sposób OpenTelemetry ułatwia śledzenie przepływu żądań?
- Jak zaimplementować idempotency i dlaczego jest ważne dla bezpieczeństwa?

### Faza 8 — Obrazy snów

**Cel:** Generowanie i przechowywanie obrazów.

- MinIO storage, API generacji (Stable Diffusion API/lokalne), limit użycia per user.  
  **DoD:** UI pokazuje obraz; plik zapisany w MinIO; limit działa.

**Co się nauczę:**

- Integracja z systemem przechowywania obiektów MinIO
- Wywoływanie API do generowania obrazów (np. Stable Diffusion)
- Implementacja limitów użycia zasobów per użytkownik

**Pytania do zadania:**

- Jak efektywnie przechowywać i udostępniać pliki multimedialne?
- Jak integrować zewnętrzne API do generowania treści?
- Jak zaprojektować mechanizm limitowania zasobów użytkownika?

### Faza 9 — Nowości Javy 1

**Cel:** Wdrożenie nowych funkcji Javy.

- Virtual Threads, Structured Concurrency, Records, Sealed.  
  **DoD:** Virtual Threads działają w I/O; testy pokazują redukcję użycia wątków.

**Co się nauczę:**

- Wykorzystanie Virtual Threads do efektywnego zarządzania wątkami
- Zastosowanie Structured Concurrency dla lepszej organizacji kodu współbieżnego
- Korzystanie z nowych konstrukcji języka: Records i Sealed Classes

**Pytania do zadania:**

- Jak Virtual Threads poprawiają skalowalność aplikacji?
- W jaki sposób Structured Concurrency ułatwia obsługę współbieżności?
- Kiedy warto stosować Records i Sealed Classes w projektach?

### Faza 10 — Kotlin

**Cel:** Porównać idiomatykę Kotlin vs Java.

- Migracja modułu do Kotlina.  
  **DoD:** Moduł działa w CI; testy przechodzą; brak regresji w e2e.

**Co się nauczę:**

- Podstawy języka Kotlin i różnice względem Javy
- Proces migracji istniejącego modułu do Kotlina
- Integracja Kotlina z istniejącym ekosystemem Spring Boot i testami

**Pytania do zadania:**

- Jakie korzyści daje Kotlin w porównaniu do Javy?
- Jak bezpiecznie migrować kod między Javą a Kotlinem?
- Jak zapewnić kompatybilność i stabilność podczas migracji?

### Faza 11 — Eksperymenty

**Cel:** Nowe technologie w trybie „lab”.

- GraphQL, Redis, Kafka, Dapr, Unleash.  
  **DoD:** Każdy eksperyment działa izolowanie, z metrykami i opisem w README.

**Co się nauczę:**

- Podstawy i zastosowania GraphQL, Redis, Kafka, Dapr i Unleash
- Izolowanie eksperymentów i integracja z istniejącą architekturą
- Monitorowanie i dokumentowanie nowych technologii w projekcie

**Pytania do zadania:**

- Jakie problemy rozwiązuje każda z tych technologii?
- Jak integrować eksperymentalne komponenty bez wpływu na stabilność?
- Jak efektywnie dokumentować i monitorować eksperymenty w projekcie?

---

## 🔍 Wyszukiwanie (FTS + trigramy)

- `to_tsvector('polish', unaccent(content))` + indeks GIN
- Triggery aktualizujące kolumnę `content_tsv`
- Fallback na trigramy (`%` operator, `similarity()`)
- Endpoint `/api/v1/dreams/search?q=…`

---

## 📡 SSE — kanał zdarzeń

- Endpoint `/api/v1/stream`
- Eventy: `ANALYSIS_READY`, `REMINDER`, `SYNC_STATUS`
- Angular wrapper: EventSource + exponential backoff

---

## 🐳 Docker Compose (skrót)

- postgres 17
- backend, frontend, ai-service
- minio
- prometheus + grafana

---

## ✅ Definition of Done (DoD)

- Każda faza kończy się działającym MVP + testami integracyjnymi
- Testcontainers → zawsze odpalalne testy w CI
- Observability → minimum dashboard + metryki
- Prywatność → eksport /me, usuwanie /me
- Audyt → tabela audit_event

---

## 📈 Status śledzenia postępu

- [ ] Faza 0 — Setup
- [ ] Faza 1 — Auth (OIDC)
- [ ] Faza 2 — CRUD snów + FTS/trgm
- [ ] Faza 3 — Nastrój i statystyki
- [ ] Faza 4 — AI-service + SSE
- [ ] Faza 5 — PWA + Offline
- [ ] Faza 6 — Powiadomienia
- [ ] Faza 7 — Observability
- [ ] Faza 8 — Obrazy snów
- [ ] Faza 9 — Java 24 features
- [ ] Faza 10 — Kotlin
- [ ] Faza 11 — Eksperymenty

---

🚀 Projekt żyje i rośnie wraz z naszymi eksperymentami — ten plik README pełni rolę **roadmapy** i checklisty do śledzenia postępu.
