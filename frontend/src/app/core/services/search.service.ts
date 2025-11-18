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
  switchMap,
} from 'rxjs';
import {MIN_QUERY_LENGTH, SEARCH_DEBOUNCE_MS, SEARCH_RELEVANT_ROUTES} from '../constants/search.constants';
import {Dream} from '../models/dream';
import {DreamsService} from './dreams.service';

/**
 * View model for search state.
 * Provides stable contract for components.
 */
export interface SearchViewModel {
  query: string;
  results: Dream[];
  loading: boolean;
  isSearching: boolean;
}

/**
 * Search service - handles search query state and execution.
 */
@Injectable({providedIn: 'root'})
export class SearchService {
  private readonly dreamsService = inject(DreamsService);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  // Private state
  private readonly querySubject = new BehaviorSubject<string>('');

  // Public observables
  readonly query$: Observable<string> = this.querySubject.asObservable();

  readonly isSearching$: Observable<boolean> = this.query$.pipe(
    map((q) => q.trim().length >= MIN_QUERY_LENGTH),
    distinctUntilChanged(),
  );

  // Debounced search execution
  private readonly debouncedQuery$ = this.querySubject.pipe(
    debounceTime(SEARCH_DEBOUNCE_MS),
    map((q) => q.trim()),
    distinctUntilChanged(),
  );

  // Search results - returns empty array when not searching
  readonly results$: Observable<Dream[]> = this.debouncedQuery$.pipe(
    switchMap((query) => {
      if (!query || query.length < MIN_QUERY_LENGTH) {
        return of([]);
      }
      return this.dreamsService.search(query).pipe(
        catchError((err) => {
          console.error('Search failed:', err);
          return of([]);
        }),
      );
    }),
  );

  // Loading state - true when search is in flight
  readonly loading$: Observable<boolean> = this.debouncedQuery$.pipe(
    switchMap((query) => {
      if (!query || query.length < MIN_QUERY_LENGTH) {
        return of(false);
      }
      // Signal loading until search completes
      return this.dreamsService.search(query).pipe(
        map(() => false),
        catchError(() => of(false)),
      );
    }),
  );

  // Unified view model
  readonly vm$: Observable<SearchViewModel> = combineLatest({
    query: this.query$,
    results: this.results$,
    loading: this.loading$,
    isSearching: this.isSearching$,
  });

  constructor() {
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

  setQuery(query: string): void {
    this.querySubject.next(query.trim());
  }

  clearSearch(): void {
    this.querySubject.next('');
  }

  private isSearchRelevantRoute(url: string): boolean {
    return SEARCH_RELEVANT_ROUTES.some((route) => url.startsWith(route));
  }
}
