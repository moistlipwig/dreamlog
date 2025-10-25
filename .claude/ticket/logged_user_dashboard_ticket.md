# 🎫 Dashboard dla zalogowanych użytkowników

> **Wersja:** 1.0  
> **Utworzono:** 2025-01-22  
> **Aktualny stage:** 🔵 **STAGE 0: Planowanie**

---

## 📊 Tracker postępu

| Stage | Nazwa                         | Status   | Czas faktyczny |
|-------|-------------------------------|----------|----------------|
| 0     | Planowanie i analiza          | 🟢 Done  | ~30min         |
| 1     | Backend API (pagination + stats) | 🟢 Done  | ~1h            |
| 2     | Frontend: Cleanup duplikacji  | 🟢 Done  | ~30min         |
| 3     | Frontend: Services i modele   | 🟢 Done  | ~20min         |
| 4     | Frontend: Komponenty UI (Dashboard) | 🟢 Done  | ~45min         |
| 5     | Routing i logout button       | 🟢 Done  | ~20min         |
| 6     | Weryfikacja (build, typecheck, lint) | 🟢 Done  | ~15min         |

**Statusy:** 🔵 W trakcie | 🟢 Done | ⚪ Oczekuje | 🔴 Blocked

**Szacowany czas:** ~9.5h | **Faktyczny czas:** ~3h 10min ✅

---

## 🎯 Cel

Dashboard jako główny ekran po zalogowaniu, wyświetlający:

- Personalizowane powitanie + quick actions (nowy sen, generuj obraz)
- Statystyki użytkownika (liczba snów, streak dni, liczba analiz, najczęstszy mood)
- Timeline ostatnich snów (max 3 najnowsze)
- Feed ostatnich aktywności (max 4)

**Design reference:** `dashboard-v2-production.html`

**Dlaczego:** Użytkownik po zalogowaniu potrzebuje szybkiego przeglądu swojej aktywności + łatwy dostęp do głównych akcji.

---

## 🏗️ Architektura

**Dotknięte moduły:**

- [x] Backend (`backend/src/main/java/pl/kalin/dreamlog/`)
- [x] Frontend (`frontend/src/app/`)
- [ ] Database (migracje - tylko jeśli backend wymaga nowych tabel)
- [ ] Infrastructure

**Kluczowe decyzje:**

1. **Frontend: Dzielimy PO FUNKCJI, nie po statusie logowania**

- `features/dashboard/` (authGuard)
- `features/dreams/` (authGuard)
- `features/landing/` (loggedInGuard - tylko wylogowani)
- **Uzasadnienie:** Lepsze lazy loading, łatwiejsze znalezienie kodu, guard decyduje o dostępie

2. **Backend: Nowe endpointy**

- `GET /api/stats/me` - statystyki zalogowanego użytkownika
- **Uzasadnienie:** Dashboard wymaga agregowanych danych, których `/api/dreams` nie dostarcza

3. **Frontend: Komponenty presentational vs smart**

- `DashboardPage` = smart (fetch, state, routing)
- Sub-komponenty w `components/` = presentational (input/output)
- **Uzasadnienie:** Reusability, testability, separation of concerns

---

## 🧹 STAGE 0: Planowanie i przygotowanie

**Cel:** Analiza kodu, wykrycie problemów, przygotowanie planu.

### Zadania dla AI Agent:

- [ ] Sprawdź strukturę `features/` (Glob)
- [ ] Sprawdź istniejące serwisy snów (wykryte duplikacje!)
- [ ] Sprawdź backend - czy endpointy `/api/stats` istnieją
- [ ] Zidentyfikuj wzorce w `features/login/` do naśladowania
- [ ] Zaproponuj strukturę komponentów dashboardu (lista wysokopoziomowa, details w trakcie)
- [ ] Zaktualizuj stage'e jeśli analiza wymaga zmian
- [ ] **STOP:** Uzyskaj akceptację przed STAGE 1

---

## 🔧 STAGE 1: Backend API

**Cel:** Stworzyć endpointy dla statystyk i aktywności.

**⚠️ Uwaga:** Może się okazać, że backend już ma częściowo te dane - sprawdź Swagger!

### Zadania

**1.1 Endpoint statystyk użytkownika**

Endpoint: `GET /api/stats/me`

Response:

```json
{
  "totalDreams": 42,
  "mostCommonMood": "positive"
}
```

Akcja:

- [ ] Stwórz `UserStatsDto`
- [ ] Stwórz `StatsController` z metodą `getMyStats()`
- [ ] Stwórz `StatsService` (logika agregacji z `DreamEntryRepository`)
- [ ] Dodaj testy (Spock integration test)
- [ ] Dodaj do OpenAPI spec

## 🧹 STAGE 2: Frontend cleanup

**Cel:** Usunąć duplikacje przed dodaniem nowego kodu.

### Zadania

**2.1 Cleanup duplikacji serwisów**

**Problem wykryty:** Mamy DWA serwisy do snów:

- `core/services/dreams.service.ts` (stary, model `Dream`)
- `apiV2/services/dream-entry.service.ts` (nowy, model `DreamEntry`)

Akcja:

- [ ] Usuń `core/services/dreams.service.ts`
- [ ] Przenieś `apiV2/services/dream-entry.service.ts` → `core/services/`
- [ ] Przenieś `apiV2/models/dream-entry.model.ts` → `core/models/`
- [ ] Zaktualizuj importy we wszystkich komponentach
- [ ] Usuń `core/models/dream.ts`
- [ ] Usuń folder `apiV2/`
- [ ] Verify: `grep -r "dreams.service\|apiV2" frontend/src/app/` powinien zwrócić 0 wyników

**2.2 Przygotuj strukturę dreams/**

Akcja:

- [ ] Stwórz folder `features/dreams/components/` (na przyszłość dla sub-komponentów)

- Sprwadz czy DreamEntryController zgadxa sie z dream entry services.ts

---

## 📦 STAGE 3: Frontend services i modele

**Cel:** Stworzyć warstwę komunikacji z backendem dla dashboardu.

### Zadania

**3.1 Modele TypeScript**

Akcja:

- [ ] Stwórz `core/models/user-stats.model.ts`
  - Pola: `totalDreams, streakDays, analysisCount, mostCommonMood`

**3.2 Services**

Akcja:

- [ ] Stwórz `core/services/user-stats.service.ts`
  - Metoda: `getUserStats(): Observable<UserStats>`
  - Endpoint: `GET /api/stats/me``
- [ ] Dodaj unit testy dla serwisów (Jest + HttpClientTestingModule)

---

## 🎨 STAGE 4: Frontend komponenty

**Cel:** Zbudować UI dashboardu.

### Zadania

**4.1 Smart component: DashboardPage**

Lokalizacja: `features/dashboard/dashboard-page.ts`

Odpowiedzialności:

- Fetch z 3 serwisów: `DreamEntryService`, `UserStatsService`, `ActivityService`
- Loading/error state management
- Grid layout (responsive)
- Przekazywanie danych do sub-komponentów

Akcja:

- [ ] Stwórz komponent z podstawową strukturą
- [ ] Dodaj signals: `recentDreams`, `userStats`, `activities`, `isLoading`, `error`
- [ ] Zaimplementuj `ngOnInit()` z `forkJoin()` do fetch danych
- [ ] Dodaj unit test

**4.2 Presentational components**

**Lista komponentów (details w trakcie implementacji):**

Dashboard będzie składał się z:

- Sekcja powitania + quick actions
- Grid ze statystykami (4 karty)
- Lista ostatnich snów
- Feed aktywności

**Akcja:**

- [ ] Zdecyduj podczas implementacji, czy to będą osobne komponenty czy sekcje w DashboardPage
- [ ] Jeśli osobne komponenty:
  - Stwórz w `features/dashboard/components/`
  - Każdy z `input()` dla danych, `output()` dla eventów
  - Każdy z unit testem
- [ ] Zastosuj design z `dashboard-v2-production.html`

---

## 🔗 STAGE 5: Routing i integracja

**Cel:** Podpiąć dashboard do aplikacji.

### Zadania

**5.1 Routing**

Akcja:

- [ ] Dodaj route w `app.routes.ts`:
  ```typescript
  { path: 'dashboard', component: DashboardPage }
  ```
- [ ] Zmień default redirect w `/app` children:
  ```typescript
  { path: '', redirectTo: 'dashboard', pathMatch: 'full' }
  ```
- [ ] Dodaj link w navigation menu (app-shell)

**5.2 Post-login redirect**

Akcja:

- [ ] Sprawdź `auth.service.ts` - czy po loginie kieruje na `/app`
- [ ] Przetestuj flow: Login → `/app` → redirect na `/app/dashboard`

---

## 🧪 STAGE 6: Testy

**Cel:** Pokrycie testami + weryfikacja działania.

### Zadania

**6.1 Unit testy**

Akcja:

- [ ] Testy dla `DashboardPage` (loading, data display, error handling)
- [ ] Testy dla każdego sub-komponentu (jeśli są)
- [ ] Testy dla `UserStatsService`
- [ ] Testy dla `ActivityService`

**6.2 Integration test**

Scenariusz:

1. Mock zalogowanego usera
2. Navigate do `/app/dashboard`
3. Verify loading state
4. Verify data displayed po fetch

Akcja:

- [ ] Stwórz integration test dla dashboard flow

**6.3 Verification**

Akcja:

- [ ] `npm run verify` (typecheck + build + test)
- [ ] `npm run lint`
- [ ] Coverage > 80% dla nowego kodu
- [ ] Manualne przetestowanie na localhost

---

## ✅ Definicja sukcesu

Zadanie jest ukończone, gdy:

1. ✅ Backend zwraca dane ze `/api/stats/me`
2. ✅ Po zalogowaniu użytkownik widzi dashboard pod `/app/dashboard`
3. ✅ Dashboard pokazuje:

- Powitanie z imieniem użytkownika
- 4 statystyki (sny, streak, analizy, mood)
- Ostatnie 3 sny z linkami do szczegółów
- Ostatnie 4 aktywności

4. ✅ Kliknięcie w sen → routing do `/app/dreams/:id`
5. ✅ Przycisk "Nowy sen" → routing do `/app/dreams/new`
6. ✅ Dashboard jest responsywny (mobile/tablet/desktop)
7. ✅ `npm run verify` i `./gradlew test` przechodzą
8. ✅ Coverage > 80%
9. ✅ Brak duplikacji serwisów w kodzie
10. ✅ Kod zgodny z guidelines (OnPush, signals, input/output functions)

---

## 🔍 Pre-implementation Analysis

**✅ WYPEŁNIONE przez AI Agent w STAGE 0 (2025-01-23)**

### Znalezione wzorce do reużycia:

**Backend:**

- ✅ Controller pattern: `DreamEntryController.java:26` - `@RestController`, `@RequiredArgsConstructor`, `getCurrentUser(Authentication)`
- ✅ Service pattern: `DreamService.java:23` - `@Service`, `@Transactional`, Lombok
- ✅ Repository pattern: `DreamEntryRepository.java:10` - extends `JpaRepository`
- ✅ Spring Data JPA - wspiera `Pageable` out-of-the-box
- ✅ Spock tests pattern: `IntegrationSpec` base class dla testów integracyjnych

**Frontend:**

- ✅ Smart component: `features/login/login-page.component.ts:24` - signals, inject(), OnPush
- ✅ Presentational: `features/login/components/login-form/` - input/output functions
- ✅ Service: `core/services/auth.service.ts:37` - providedIn root, inject(), Observable patterns
- ✅ Guard: `core/guards/auth-guard.ts:8` - functional guard z inject()
- ✅ Material Design: `dream-list.html:5` - mat-card, TailwindCSS grid, skeleton loading
- ✅ AppShell layout: `app-shell.ts:40` - mat-toolbar + mat-sidenav + router-outlet
- ✅ Test: `*.spec.ts` obok komponentów

### Wykryte problemy i konflikty (Boy Scout Rule!):

#### ⚠️ PROBLEM 1: Brak paginacji na backendzie

**Lokalizacja:** `DreamEntryController.java:38`, `DreamService.java:37`, `DreamEntryRepository.java:17`

**Problem:**

- Endpoint `GET /api/dreams` zwraca **WSZYSTKIE** sny użytkownika jako `List<DreamResponse>`
- Gdy użytkownik ma 1000 snów → 1000 rekordów w jednym requeście
- Brak parametrów `page`, `size`, `sort`
- `findByUserId()` zwraca `List` zamiast `Page`

**Wpływ na ticket:** Dashboard potrzebuje tylko 3 ostatnich snów, ale dostanie wszystkie!

**Rozwiązanie:**

```java
// Backend changes:
// 1. DreamEntryRepository.findByUserId() → return Page<DreamEntry>
// 2. DreamService.getUserDreams() → accept Pageable parameter
// 3. DreamEntryController.getUserDreams() → add @RequestParam Pageable
```

**Akcja:** Dodać do STAGE 1 jako zadanie 1.0 (przed stats endpoint)

---

#### ⚠️ PROBLEM 2: Dream model incompatibility

**Lokalizacja:** `frontend/src/app/core/models/dream.ts:1` vs `backend/.../DreamResponse.java:14`

**Problem:**

- Frontend `Dream` interface: `id, title, content, date, tags, mood` (6 pól)
- Backend `DreamResponse`: `id, date, title, content, moodInDream, moodAfterDream, vividness, lucid, tags` (9 pól)
- Frontend brakuje: `moodInDream`, `moodAfterDream`, `vividness`, `lucid`
- Frontend ma: `mood: number` (przestarzałe)

**Wpływ na ticket:** Dashboard nie będzie mógł wyświetlić pełnych informacji o śnie

**Rozwiązanie:**

```typescript
// Zaktualizować core/models/dream.ts aby pasował do backend:
export interface Dream {
  id: string;
  date: string; // ISO
  title: string;
  content: string;
  moodInDream: 'POSITIVE' | 'NEUTRAL' | 'NEGATIVE' | 'NIGHTMARE' | 'MIXED' | null;
  moodAfterDream: 'POSITIVE' | 'NEUTRAL' | 'NEGATIVE' | 'NIGHTMARE' | 'MIXED' | null;
  vividness: number; // 1-10
  lucid: boolean;
  tags: string[];
}
```

**Akcja:** Dodać do STAGE 3 jako zadanie 3.1

---

#### ⚠️ PROBLEM 3: DreamsService.list() nie przyjmuje parametrów

**Lokalizacja:** `core/services/dreams.service.ts:11`

**Problem:**

```typescript
list()
:
Observable < Dream[] > {
  return this.api.get<Dream[]>('/dreams');
}
```

- Nie można przekazać `page`, `size`, `sort`
- Zwraca `Dream[]` zamiast `PagedResponse<Dream>`

**Rozwiązanie:**

- Dodać metodę `listPaginated(page: number, size: number): Observable<PagedResponse<Dream>>`
- Lub zmienić `list()` aby przyjmowała opcjonalne parametry

**Akcja:** Dodać do STAGE 3 jako zadanie 3.2

---

#### ⚠️ PROBLEM 4: Duplikacja serwisów (już w tickecie, ale z dodatkowymi szczegółami)

**Lokalizacja:**

- `core/services/dreams.service.ts` (stary, używa `/dreams`, model `Dream`)
- `apiV2/services/dream-entry.service.ts` (nowy, używa `/api/dreams`, model `DreamEntry`)

**Problem wykryty przez Grep:**

- 2 różne modele: `Dream` vs `DreamEntry`
- 2 różne ścieżki API: `/dreams` vs `/api/dreams`
- Obecnie używane komponenty: `DreamList`, `DreamDetail`, `DreamEdit` używają starego `dreams.service.ts`

**Rozwiązanie:** Potwierdzam plan z ticketu (STAGE 2) - usunąć stare, przenieść nowe

---

#### ⚠️ PROBLEM 5: Brak przycisku Logout w AppShell

**Lokalizacja:** `app-shell.html:18`

**Problem:**

- Toolbar ma tylko: menu (mobile), "Dreamlog" logo, search bar, settings button
- Brak sposobu na wylogowanie się z aplikacji (poza `/api/auth/logout` w dev tools)
- `AuthService.logout()` istnieje ale nigdzie nie jest wywoływany

**Wpływ na UX:** Użytkownik nie może się wylogować!

**Rozwiązanie:** Dodać przycisk/menu z opcją logout w toolbar

**Akcja:** Dodać jako STAGE 5.5 (po routing, przed testy)

---

#### ℹ️ INFO 6: Routing - `/app` default jest `/app/dreams`

**Lokalizacja:** `app.routes.ts:27`

**Obecny stan:**

```typescript
{
  path: 'app', component
:
  AppShell, canActivate
:
  [authGuard], children
:
  [
    {path: '', redirectTo: 'dreams', pathMatch: 'full'}, // ← domyślnie dreams
    {path: 'dreams', component: DreamList},
    ...
  ]
}
```

**Zgodne z planem:** Ticket przewiduje zmianę na `redirectTo: 'dashboard'` w STAGE 5

---

### Proponowany refactoring:

**Obowiązkowe (potrzebne do działania dashboardu):**

- 🧹 **STAGE 1.0 (NOWE):** Dodać paginację do backendu (`Page<DreamResponse>`, `Pageable` params)
- 🧹 **STAGE 2:** Usunięcie starego `dreams.service.ts` i folderu `apiV2/` (zgodnie z ticketem)
- 🧹 **STAGE 3.1 (ROZSZERZONE):** Zaktualizować model `Dream` aby pasował do backend `DreamResponse`
- 🧹 **STAGE 3.2 (NOWE):** Dodać metodę `listPaginated()` do serwisu

**Opcjonalne (UX improvements):**

- 🧹 **STAGE 5.5 (NOWE):** Dodać przycisk Logout w AppShell toolbar

### Backend endpoints do zaimplementowania:

**Nowe:**

- `GET /api/dreams?page=0&size=5&sort=date,desc` - zmienić z `List<>` na `Page<>`
- `GET /api/stats/me` - agregacja z `DreamEntry` (statystyki dashboardu)

**Istniejące (do modyfikacji):**

- `GET /api/dreams` - dodać parametry paginacji, zmienić return type na `Page<>`

### Estymacja czasu (zaktualizowana):

| Stage | Opis              | Status |
|-------|-------------------|--------|
| 0     | Planowanie        | ✅ Done |
| 1     | Backend API       |
| 2     | Frontend cleanup  |
| 3     | Services i modele |
| 4     | Komponenty UI     |
| 5     | Routing           |
| 6     | Testy             |

**Uzasadnienie +2h:** Dodatkowe problemy wymagają czasu na refactoring istniejącego kodu (paginacja, model compatibility, logout)

### Rekomendacje przed rozpoczęciem implementacji:

**✅ Zatwierdzam strukturę z ticketu** - podział na stage'y jest dobry!

**⚠️ Proponuję zmiany:**

1. Dodać zadanie "1.0 Backend paginacja" PRZED "1.1 Endpoint statystyk"
2. Rozszerzyć STAGE 3 o zadania dot. modelu Dream i paginacji
3. Dodać zadanie "5.5 Logout button" po routing
4. Zaktualizować estymację czasu: 9.5h → 11.5h

---

## 📝 Notatki

### 2025-01-23 (Implementation - COMPLETED ✅):

**Co zostało zaimplementowane:**

**Backend:**
1. ✅ Paginacja dla `/api/dreams` - zwraca `Page<DreamResponse>` z parametrami `?page=0&size=20&sort=date,desc`
2. ✅ Endpoint `/api/stats/me` - zwraca statystyki użytkownika (totalDreams, mostCommonMood)
3. ✅ UserStatsDto, StatsService, StatsController
4. ✅ Testy Spock dla paginacji (4 testy) i statystyk (5 testów)

**Frontend:**
1. ✅ Cleanup duplikacji - usunięty stary `dreams.service.ts` i folder `apiV2/`
2. ✅ Zaktualizowany model `Dream` - zgodny z backend (Mood enum: POSITIVE, NEUTRAL, NEGATIVE, NIGHTMARE, MIXED)
3. ✅ Dodany `PagedResponse<T>` interface dla odpowiedzi stronicowanych
4. ✅ DreamsService z paginacją `list(page, size, sort)`
5. ✅ UserStatsService z `getMyStats()`
6. ✅ ApiHttp rozszerzony o `put()` i `delete()` metody
7. ✅ DashboardPage component:
   - Powitanie użytkownika
   - 2 stat cards (Total Dreams, Most Common Mood)
   - Lista 3 ostatnich snów
   - Empty state gdy brak snów
   - Przycisk FAB "Dodaj sen"
   - Loading i error states
8. ✅ Routing: `/app` → redirect na `/app/dashboard`
9. ✅ Dodany przycisk Logout w toolbar
10. ✅ Zaktualizowany sidenav z Dashboard link

**Weryfikacja:**
- ✅ ESLint passed (auto-fixed imports)
- ✅ TypeScript compilation passed
- ✅ Production build passed (946.91 kB → 194.64 kB gzipped)

**Nierozwiązane issues (do przyszłych ticketów):**
- ⚠️ DreamEdit form używa starego modelu (pojedynczy `mood: number` zamiast `moodInDream/moodAfterDream`)
- ⚠️ Backend testy nie uruchomione lokalnie (problem z gradlew w Git Bash na Windows)

### 2025-01-22 (Planning):

- Struktura komponentów: Zostawiamy jako high-level lista, details podczas implementacji
- Nie definiujemy z góry wszystkich sub-komponentów - może się okazać, że wystarczy DashboardPage z sekcjami

---

## 🚀 Co dalej?

Po ukończeniu dashboardu:

1. Rozbudowa widoku szczegółów snu (`/app/dreams/:id`)
2. Formularz tworzenia snu (`/app/dreams/new`)
3. Wyszukiwarka snów (full-text search)
4. Profil użytkownika i ustawienia
5. Notatki sporzadac do kolejnych prac jezeli uznasz za potrzebne zostaw w ticket_notes.md obok.

---

**Status dokumentu:** 🔵 Draft | Wymaga analizy AI w STAGE 0 → Akceptacja użytkownika → Implementacja
