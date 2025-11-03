import {ChangeDetectionStrategy, Component, inject, OnDestroy, OnInit} from '@angular/core';
import {FormControl, ReactiveFormsModule} from '@angular/forms';
import {MatButtonModule} from '@angular/material/button';
import {MatFormFieldModule} from '@angular/material/form-field';
import {MatIconModule} from '@angular/material/icon';
import {MatInputModule} from '@angular/material/input';
import {debounceTime, distinctUntilChanged, Subscription} from 'rxjs';

import {SearchService} from './services/search.service';

/**
 * Reusable search bar component with debouncing and clear functionality.
 * Triggers search only when user types 3+ characters.
 * Syncs bidirectionally with SearchService query state.
 */
@Component({
  selector: 'app-search-bar',
  imports: [
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatIconModule,
    MatButtonModule,
  ],
  templateUrl: './search-bar.html',
  styleUrls: ['./search-bar.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SearchBar implements OnInit, OnDestroy {
  private search = inject(SearchService);
  control = new FormControl('');
  private subscriptions = new Subscription();

  ngOnInit(): void {
    // Sync FormControl with current SearchService query on init
    this.subscriptions.add(
      this.search.query$.subscribe((query) => {
        // Only update if different to avoid circular updates
        if (this.control.value !== query) {
          this.control.setValue(query, {emitEvent: false});
        }
      }),
    );

    // Emit FormControl changes to SearchService (debounced)
    this.subscriptions.add(
      this.control.valueChanges
        .pipe(debounceTime(500), distinctUntilChanged())
        .subscribe((query) => {
          const trimmedQuery = query?.trim() ?? '';
          // Only search if 3+ characters or empty (to reset)
          if (trimmedQuery.length === 0 || trimmedQuery.length >= 3) {
            this.search.setQuery(trimmedQuery);
          }
        }),
    );
  }

  ngOnDestroy(): void {
    this.subscriptions.unsubscribe();
  }

  /**
   * Clear search input and reset query
   */
  clear(): void {
    this.control.setValue('');
    this.search.setQuery('');
  }

  /**
   * Check if clear button should be shown
   */
  get showClear(): boolean {
    return !!this.control.value && this.control.value.length > 0;
  }
}
