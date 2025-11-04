import {DestroyRef, inject, Injectable} from '@angular/core';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {NavigationEnd, Router} from '@angular/router';
import {
  BehaviorSubject,
  catchError,
  combineLatest,
  debounceTime,
  distinctUntilChanged,
  filter,
  map,
  Observable,
  of,
  shareReplay,
  startWith,
  switchMap,
} from 'rxjs';
import {MIN_QUERY_LENGTH, SEARCH_DEBOUNCE_MS, SEARCH_RELEVANT_ROUTES} from '../constants/search.constants';
import {Dream} from '../models/dream';
import {DreamsService} from './dreams.service';

/**
 * Unified view model for search state.
 * Provides stable contract for components to consume.
 */
export interface SearchViewModel {
  query: string;
  results: Dream[];
  loading: boolean;
  isSearching: boolean;
}

/**
 * Search facade service implementing facade pattern.
 * Provides unified search state management with reactive streams.
 *
 * Key principles:
 * - Open/Closed: Exposes readonly observables, hides implementation
 * - Single Responsibility: Pure reactive pipelines, no imperative side effects
 * - Dependency Inversion: Components depend on vm$ contract, not internals
 */
@Injectable({providedIn: 'root'})
export class SearchService {
  private readonly dreamsService = inject(DreamsService);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  // Private state - NEVER expose these directly
  private readonly querySubject = new BehaviorSubject<string>('');
  private readonly baseResultsSubject = new BehaviorSubject<Dream[]>([]);

  // Public readonly observables (Open/Closed principle)
  readonly query$: Observable<string> = this.querySubject.asObservable();

  // Derive results purely from reactive pipeline (no imperative next())
  readonly results$: Observable<Dream[]> = this.querySubject.pipe(
    debounceTime(SEARCH_DEBOUNCE_MS),
    map((q) => q.trim()),
    distinctUntilChanged(),
    switchMap((query) => {
      if (!query || query.length < MIN_QUERY_LENGTH) {
        // Return base results (all dreams) when not searching
        return this.baseResultsSubject.asObservable();
      }
      // Perform search
      return this.dreamsService.search(query).pipe(
        catchError((err) => {
          console.error('Search failed:', err);
          return of([]);
        }),
      );
    }),
    shareReplay({bufferSize: 1, refCount: true}),
  );

  readonly loading$: Observable<boolean> = this.querySubject.pipe(
    debounceTime(SEARCH_DEBOUNCE_MS),
    map((q) => q.trim()),
    distinctUntilChanged(),
    switchMap((query) => {
      if (!query || query.length < MIN_QUERY_LENGTH) {
        return of(false);
      }
      // Emit true immediately, then false when results arrive
      return this.dreamsService.search(query).pipe(
        map(() => false),
        catchError(() => of(false)),
        startWith(true),
      );
    }),
    shareReplay({bufferSize: 1, refCount: true}),
  );

  readonly isSearching$: Observable<boolean> = this.query$.pipe(
    map((q) => q.trim().length >= MIN_QUERY_LENGTH),
    distinctUntilChanged(),
  );

  // Unified view model for components (Interface Segregation principle)
  readonly vm$: Observable<SearchViewModel> = combineLatest({
    query: this.query$,
    results: this.results$,
    loading: this.loading$,
    isSearching: this.isSearching$,
  }).pipe(shareReplay({bufferSize: 1, refCount: true}));

  constructor() {
    // Clear search when leaving relevant routes (lifecycle-managed)
    this.router.events
      .pipe(
        filter((event): event is NavigationEnd => event instanceof NavigationEnd),
        map((event) => this.isSearchRelevantRoute(event.url)),
        distinctUntilChanged(),
        filter((isRelevant) => !isRelevant),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(() => this.clearSearch());
  }

  /**
   * Set the search query. This will automatically trigger a search if query meets minimum length.
   */
  setQuery(query: string): void {
    this.querySubject.next(query.trim());
  }

  /**
   * Clear search query and reset to base results.
   */
  clearSearch(): void {
    this.querySubject.next('');
  }

  /**
   * Set base results shown when not searching (e.g., all dreams).
   * These are displayed when query is empty or below minimum length.
   */
  setBaseResults(dreams: Dream[]): void {
    this.baseResultsSubject.next(dreams);
  }

  private isSearchRelevantRoute(url: string): boolean {
    return SEARCH_RELEVANT_ROUTES.some((route) => url.startsWith(route));
  }
}
