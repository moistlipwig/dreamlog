import {ChangeDetectionStrategy, Component, inject} from '@angular/core';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {FormControl, ReactiveFormsModule} from '@angular/forms';
import {MatButtonModule} from '@angular/material/button';
import {MatFormFieldModule} from '@angular/material/form-field';
import {MatIconModule} from '@angular/material/icon';
import {MatInputModule} from '@angular/material/input';
import {distinctUntilChanged} from 'rxjs';

import {SearchService} from './services/search.service';

/**
 * Reusable search bar component.
 * Debouncing is handled by SearchService - this component just forwards input.
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
  private readonly search = inject(SearchService);
  readonly control = new FormControl('');

  constructor() {
    // Sync SearchService query to FormControl
    this.search.query$.pipe(takeUntilDestroyed()).subscribe((query) => {
      if (this.control.value !== query) {
        this.control.setValue(query, {emitEvent: false});
      }
    });

    this.control.valueChanges
      .pipe(distinctUntilChanged(), takeUntilDestroyed())
      .subscribe((query) => {
        this.search.setQuery(query?.trim() ?? '');
      });
  }

  clear(): void {
    this.control.setValue('');
    this.search.clearSearch();
  }

  get showClear(): boolean {
    return !!this.control.value && this.control.value.length > 0;
  }
}
