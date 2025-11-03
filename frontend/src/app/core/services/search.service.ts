import {inject, Injectable} from '@angular/core';
import {NavigationEnd, Router} from '@angular/router';
import {BehaviorSubject, debounceTime, distinctUntilChanged, filter, switchMap} from 'rxjs';
import {Dream} from '../models/dream';
import {DreamsService} from './dreams.service';

@Injectable({providedIn: 'root'})
export class SearchService {
  private readonly dreamsService = inject(DreamsService);
  private readonly router = inject(Router);

  private query = new BehaviorSubject<string>('');
  private results = new BehaviorSubject<Dream[]>([]);
  private loading = new BehaviorSubject<boolean>(false);

  readonly query$ = this.query.asObservable();
  readonly results$ = this.results.asObservable();
  readonly loading$ = this.loading.asObservable();

  constructor() {
    // Set up automatic search when query changes
    this.query$
      .pipe(
        debounceTime(300), // Debounce to avoid excessive API calls
        distinctUntilChanged(), // Only trigger if query actually changed
        switchMap((q) => {
          if (q && q.trim().length >= 3) {
            this.loading.next(true);
            return this.dreamsService.search(q);
          }
          // Clear results if query is too short or empty
          this.loading.next(false);
          this.results.next([]);
          return [];
        }),
      )
      .subscribe({
        next: (dreams) => {
          this.results.next(dreams);
          this.loading.next(false);
        },
        error: () => {
          this.results.next([]);
          this.loading.next(false);
        },
      });

    // Clear search when navigating away from search-relevant routes
    this.router.events
      .pipe(filter((event) => event instanceof NavigationEnd))
      .subscribe((event: NavigationEnd) => {
        const searchRelevantRoutes = ['/app/dreams', '/app/search', '/app/dashboard'];
        const isSearchRelevant = searchRelevantRoutes.some((route) => event.url.startsWith(route));

        // Clear search if navigating away from search-relevant routes
        if (!isSearchRelevant) {
          this.clearSearch();
        }
      });
  }

  /**
   * Set the search query. This will automatically trigger a search if query is 3+ characters.
   */
  setQuery(q: string): void {
    this.query.next(q.trim());
  }

  /**
   * Clear search query and results.
   */
  clearSearch(): void {
    this.query.next('');
    this.results.next([]);
    this.loading.next(false);
  }

  /**
   * Manually set results (for non-search contexts like listing all dreams).
   */
  setResults(dreams: Dream[]): void {
    this.results.next(dreams);
  }
}
