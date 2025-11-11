import {DatePipe} from '@angular/common';
import {ChangeDetectionStrategy, Component, computed, inject, signal} from '@angular/core';
import {toSignal} from '@angular/core/rxjs-interop';
import {MatButtonModule} from '@angular/material/button';
import {MatCardModule} from '@angular/material/card';
import {MatIconModule} from '@angular/material/icon';
import {RouterLink} from '@angular/router';
import {catchError, of} from 'rxjs';

import {Dream} from '../../core/models/dream';
import {SearchBar} from '../../core/search-bar';
import {DreamsService} from '../../core/services/dreams.service';
import {SearchService} from '../../core/services/search.service';
import {EmptyState} from '../../shared/empty-state';
import {TagChips} from '../../shared/tag-chips';
import {getMoodClass} from '../../shared/utils/mood.utils';

@Component({
  selector: 'app-dream-list',
  imports: [
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    RouterLink,
    TagChips,
    DatePipe,
    EmptyState,
    SearchBar,
  ],
  templateUrl: './dream-list.html',
  styleUrls: ['./dream-list.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DreamList {
  private readonly dreamsService = inject(DreamsService);
  private readonly searchService = inject(SearchService);

  // Search state with safe initial value
  private readonly searchVm = toSignal(this.searchService.vm$, {
    initialValue: {
      query: '',
      results: [],
      loading: false,
      isSearching: false,
    },
  });

  // Local state - all dreams loaded from API
  private readonly allDreamsFromApi = signal<Dream[]>([]);
  readonly isLoading = signal(true);
  readonly error = signal<string | null>(null);

  // View model for template - combines search and local state
  readonly vm = computed(() => {
    const search = this.searchVm();
    const allDreams = this.allDreamsFromApi();

    return {
      query: search.query,
      results: search.isSearching ? search.results : allDreams,
      loading: search.isSearching ? search.loading : this.isLoading(),
      isSearching: search.isSearching,
    };
  });

  readonly skeletons = Array.from({length: 4});

  // Pure function from utils (DRY principle)
  readonly getMoodClass = getMoodClass;

  constructor() {
    // Load all dreams on component init
    this.loadAllDreams();
  }

  private loadAllDreams(): void {
    this.isLoading.set(true);
    this.error.set(null);

    this.dreamsService
      .list(0, 20, 'date,desc')
      .pipe(
        catchError((err) => {
          console.error('Failed to load dreams:', err);
          return of({
            content: [],
            totalElements: 0,
            totalPages: 0,
            size: 20,
            number: 0,
            first: true,
            last: true,
            empty: true,
          });
        }),
      )
      .subscribe((page) => {
        this.allDreamsFromApi.set(page.content);
        this.isLoading.set(false);
      });
  }
}
