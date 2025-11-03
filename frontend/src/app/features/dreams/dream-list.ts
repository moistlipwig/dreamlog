import {AsyncPipe, DatePipe} from '@angular/common';
import {ChangeDetectionStrategy, Component, inject, OnInit} from '@angular/core';
import {MatButtonModule} from '@angular/material/button';
import {MatCardModule} from '@angular/material/card';
import {MatIconModule} from '@angular/material/icon';
import {RouterLink} from '@angular/router';
import {combineLatest, map} from 'rxjs';

import {Mood} from '../../core/models/dream';
import {SearchBar} from '../../core/search-bar';
import {DreamsService} from '../../core/services/dreams.service';
import {SearchService} from '../../core/services/search.service';
import {EmptyState} from '../../shared/empty-state';
import {TagChips} from '../../shared/tag-chips';

@Component({
  selector: 'app-dream-list',
  imports: [
    AsyncPipe,
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
export class DreamList implements OnInit {
  private dreamsService = inject(DreamsService);
  private searchService = inject(SearchService);

  // Use centralized search results from SearchService
  dreams$ = combineLatest([this.searchService.results$, this.searchService.query$]).pipe(
    map(([results]) => {
      // If there's an active search query (3+ chars), show search results
      // Otherwise show results that were manually set (all dreams)
      return results;
    }),
  );

  loading$ = this.searchService.loading$;
  skeletons = Array.from({length: 4});

  ngOnInit(): void {
    // Load all dreams on init if no active search
    combineLatest([this.searchService.query$, this.searchService.results$])
      .pipe(
        map(([query, results]) => ({query, results, isEmpty: results.length === 0})),
      )
      .subscribe(({query, isEmpty}) => {
        // Only fetch all dreams if there's no query and no results yet
        if (!query && isEmpty) {
          this.dreamsService.list().subscribe((page) => {
            this.searchService.setResults(page.content);
          });
        }
      });
  }

  getMoodClass(mood: Mood | null | undefined): string {
    if (!mood) return 'mood-neutral';

    const moodMap: Record<Mood, string> = {
      [Mood.POSITIVE]: 'mood-happy',
      [Mood.NEUTRAL]: 'mood-neutral',
      [Mood.NEGATIVE]: 'mood-sad',
      [Mood.NIGHTMARE]: 'mood-anxious',
      [Mood.MIXED]: 'mood-neutral',
    };

    return moodMap[mood] || 'mood-neutral';
  }
}
