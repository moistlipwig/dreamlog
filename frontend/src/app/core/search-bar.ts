import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { debounceTime } from 'rxjs';

import { SearchService } from './services/search.service';

@Component({
  selector: 'app-search-bar',
  imports: [ReactiveFormsModule, MatFormFieldModule, MatInputModule, MatIconModule],
  templateUrl: './search-bar.html',
  styleUrls: ['./search-bar.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SearchBar {
  private search = inject(SearchService);
  control = new FormControl('');

  constructor() {
    this.control.valueChanges.pipe(debounceTime(300)).subscribe((q) => {
      this.search.setQuery(q ?? '');
    });
  }
}
