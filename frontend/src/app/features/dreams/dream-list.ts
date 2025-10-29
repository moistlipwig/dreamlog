import {AsyncPipe, DatePipe} from '@angular/common';
import {ChangeDetectionStrategy, Component, inject} from '@angular/core';
import {MatCardModule} from '@angular/material/card';
import {RouterLink} from '@angular/router';
import {map, switchMap} from 'rxjs';

import {Mood} from '../../core/models/dream';
import {SearchBar} from '../../core/search-bar';
import {DreamsService} from '../../core/services/dreams.service';
import {SearchService} from '../../core/services/search.service';
import {EmptyState} from '../../shared/empty-state';
import {TagChips} from '../../shared/tag-chips';

@Component({
  selector: 'app-dream-list',
  imports: [AsyncPipe, MatCardModule, RouterLink, TagChips, DatePipe, EmptyState, SearchBar],
  templateUrl: './dream-list.html',
  styleUrls: ['./dream-list.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DreamList {
  private dreamsService = inject(DreamsService);
  private searchService = inject(SearchService);

  // Switch between search results and full list based on query
  dreams$ = this.searchService.query$.pipe(
    switchMap((query) => {
      if (query && query.length >= 3) {
        // Show search results
        return this.dreamsService.search(query);
      }
      // Show all dreams
      return this.dreamsService.list().pipe(map((page) => page.content));
    }),
  );

  skeletons = Array.from({length: 4});

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
