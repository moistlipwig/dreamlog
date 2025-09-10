import { ChangeDetectionStrategy, Component } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { debounceTime } from 'rxjs';
import { SearchService } from './services/search.service';

@Component({
  selector: 'app-search-bar',
  imports: [ReactiveFormsModule, MatInputModule, MatIconModule],
  templateUrl: './search-bar.html',
  styleUrl: './search-bar.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class SearchBar {
  control = new FormControl('');

  constructor(private search: SearchService) {
    this.control.valueChanges.pipe(debounceTime(300)).subscribe((q) => {
      this.search.setQuery(q ?? '');
    });
  }
}
