import {DatePipe} from '@angular/common';
import {ChangeDetectionStrategy, Component, computed, inject, signal} from '@angular/core';
import {toSignal} from '@angular/core/rxjs-interop';
import {MatButtonModule} from '@angular/material/button';
import {MatCardModule} from '@angular/material/card';
import {MatIconModule} from '@angular/material/icon';
import {RouterLink} from '@angular/router';
import {catchError, forkJoin, of, switchMap} from 'rxjs';

import {Dream} from '../../core/models/dream';
import {UserStats} from '../../core/models/user-stats';
import {SearchBar} from '../../core/search-bar';
import {AuthService, User} from '../../core/services/auth.service';
import {DreamsService} from '../../core/services/dreams.service';
import {SearchService} from '../../core/services/search.service';
import {UserStatsService} from '../../core/services/user-stats.service';

@Component({
  selector: 'app-dashboard-page',
  imports: [MatCardModule, MatButtonModule, MatIconModule, RouterLink, DatePipe, SearchBar],
  templateUrl: './dashboard-page.html',
  styleUrls: ['./dashboard-page.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DashboardPage {
  private readonly dreamsService = inject(DreamsService);
  private readonly statsService = inject(UserStatsService);
  private readonly authService = inject(AuthService);
  private readonly searchService = inject(SearchService);

  // State signals
  user = signal<User | null>(null);
  private recentDreamsFromApi = signal<Dream[]>([]);
  stats = signal<UserStats | null>(null);
  isLoading = signal(true);
  error = signal<string | null>(null);

  // Search query as signal
  private searchQuery = toSignal(this.searchService.query$, {initialValue: ''});

  // Display dreams - switches between search results and recent dreams
  displayDreams = computed(() => {
    const query = this.searchQuery();
    if (query && query.length >= 3) {
      // Return empty for now, will be updated by search subscription
      return [];
    }
    return this.recentDreamsFromApi();
  });

  // Computed property to check if we're in search mode
  isSearching = computed(() => {
    const query = this.searchQuery();
    return query && query.length >= 3;
  });

  constructor() {
    this.loadDashboardData();
    this.setupSearch();
  }

  /**
   * Setup search subscription to update dreams when query changes
   */
  private setupSearch(): void {
    this.searchService.query$
      .pipe(
        switchMap((query) => {
          if (query && query.length >= 3) {
            return this.dreamsService.search(query);
          }
          return of([]);
        }),
      )
      .subscribe((searchResults) => {
        if (this.searchQuery() && this.searchQuery().length >= 3) {
          // Update recent dreams with search results
          this.recentDreamsFromApi.set(searchResults);
        }
      });
  }

  loadDashboardData(): void {
    this.isLoading.set(true);
    this.error.set(null);

    // Subscribe to user observable
    this.authService.user$.subscribe((user) => {
      this.user.set(user);
    });

    // Fetch recent dreams (3 most recent) and stats in parallel
    forkJoin({
      dreams: this.dreamsService.list(0, 3, 'date,desc').pipe(
        catchError((err) => {
          console.error('Failed to load recent dreams:', err);
          return of({
            content: [],
            totalElements: 0,
            totalPages: 0,
            size: 3,
            number: 0,
            first: true,
            last: true,
            empty: true,
          });
        }),
      ),
      stats: this.statsService.getMyStats().pipe(
        catchError((err) => {
          console.error('Failed to load stats:', err);
          return of({totalDreams: 0, mostCommonMood: null});
        }),
      ),
    }).subscribe({
      next: ({dreams, stats}) => {
        this.recentDreamsFromApi.set(dreams.content);
        this.stats.set(stats);
        this.isLoading.set(false);
      },
      error: (err) => {
        console.error('Dashboard load failed:', err);
        this.error.set('Failed to load dashboard data');
        this.isLoading.set(false);
      },
    });
  }

  getMoodLabel(mood: string | null): string {
    if (!mood) return 'No data';
    return mood.charAt(0) + mood.slice(1).toLowerCase();
  }

  getMoodEmoji(mood: string | null | undefined): string {
    if (!mood) return '😐';

    const moodMap: Record<string, string> = {
      HAPPY: '😊',
      SAD: '😢',
      NEUTRAL: '😐',
      EXCITED: '🤩',
      ANXIOUS: '😰',
      PEACEFUL: '😌',
      CONFUSED: '🤔',
      SCARED: '😨',
    };

    return moodMap[mood.toUpperCase()] || '😐';
  }
}
