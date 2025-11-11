import {DatePipe} from '@angular/common';
import {ChangeDetectionStrategy, Component, computed, inject, signal} from '@angular/core';
import {toSignal} from '@angular/core/rxjs-interop';
import {MatButtonModule} from '@angular/material/button';
import {MatCardModule} from '@angular/material/card';
import {MatIconModule} from '@angular/material/icon';
import {RouterLink} from '@angular/router';
import {catchError, forkJoin, of} from 'rxjs';

import {Dream} from '../../core/models/dream';
import {UserStats} from '../../core/models/user-stats';
import {SearchBar} from '../../core/search-bar';
import {AuthService} from '../../core/services/auth.service';
import {DreamsService} from '../../core/services/dreams.service';
import {SearchService} from '../../core/services/search.service';
import {UserStatsService} from '../../core/services/user-stats.service';
import {getMoodEmoji, getMoodLabel} from '../../shared/utils/mood.utils';

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

  // Search state with safe initial value
  readonly searchVm = toSignal(this.searchService.vm$, {
    initialValue: {
      query: '',
      results: [],
      loading: false,
      isSearching: false,
    },
  });

  // Local dashboard state
  readonly user = toSignal(this.authService.user$);
  private readonly recentDreamsFromApi = signal<Dream[]>([]);
  readonly stats = signal<UserStats | null>(null);
  readonly isLoading = signal(true);
  readonly error = signal<string | null>(null);

  // Display logic: show search results when searching, otherwise show recent dreams
  readonly displayDreams = computed(() => {
    const vm = this.searchVm();
    return vm.isSearching ? vm.results : this.recentDreamsFromApi();
  });

  // Pure functions from utils (DRY principle)
  readonly getMoodLabel = getMoodLabel;
  readonly getMoodEmoji = getMoodEmoji;

  constructor() {
    this.loadDashboardData();
  }

  loadDashboardData(): void {
    this.isLoading.set(true);
    this.error.set(null);

    // Fetch recent dreams and stats in parallel
    forkJoin({
      dreams: this.dreamsService.list(0, 5, 'date,desc').pipe(
        catchError((err) => {
          console.error('Failed to load recent dreams:', err);
          return of({
            content: [],
            totalElements: 0,
            totalPages: 0,
            size: 5,
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
}
