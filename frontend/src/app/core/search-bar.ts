import {ChangeDetectionStrategy, Component, inject} from '@angular/core';
import {FormControl, ReactiveFormsModule} from '@angular/forms';
import {MatButtonModule} from '@angular/material/button';
import {MatFormFieldModule} from '@angular/material/form-field';
import {MatIconModule} from '@angular/material/icon';
import {MatInputModule} from '@angular/material/input';
import {debounceTime, distinctUntilChanged} from 'rxjs';

import {SearchService} from './services/search.service';

/**
 * Reusable search bar component with debouncing and clear functionality.
 * Triggers search only when user types 3+ characters.
 * Updates SearchService query state for consumption by parent components.
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
export class SearchBar {
  private search = inject(SearchService);
  control = new FormControl('');

  constructor() {
    // Debounce for 500ms and only emit when value actually changes
    this.control.valueChanges
      .pipe(debounceTime(500), distinctUntilChanged())
      .subscribe((query) => {
        const trimmedQuery = query?.trim() ?? '';
        // Only search if 3+ characters or empty (to reset)
        if (trimmedQuery.length === 0 || trimmedQuery.length >= 3) {
          this.search.setQuery(trimmedQuery);
        }
      });
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
