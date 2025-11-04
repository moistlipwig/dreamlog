import {AsyncPipe, DatePipe} from '@angular/common';
import {ChangeDetectionStrategy, Component, inject} from '@angular/core';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {MatButtonModule} from '@angular/material/button';
import {MatCardModule} from '@angular/material/card';
import {MatIconModule} from '@angular/material/icon';
import {RouterLink} from '@angular/router';

import {SearchBar} from '../../core/search-bar';
import {DreamsService} from '../../core/services/dreams.service';
import {SearchService} from '../../core/services/search.service';
import {EmptyState} from '../../shared/empty-state';
import {TagChips} from '../../shared/tag-chips';
import {getMoodClass} from '../../shared/utils/mood.utils';

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
export class DreamList {
  private readonly dreamsService = inject(DreamsService);
  private readonly searchService = inject(SearchService);

  // Consume unified view model from facade (Single Responsibility)
  readonly vm$ = this.searchService.vm$;

  readonly skeletons = Array.from({length: 4});

  // Pure function from utils (DRY principle)
  readonly getMoodClass = getMoodClass;

  constructor() {
    // Preload all dreams on component init and set as base results
    this.dreamsService
      .list(0, 20, 'date,desc')
      .pipe(takeUntilDestroyed())
      .subscribe((page) => {
        this.searchService.setBaseResults(page.content);
      });
  }
}
