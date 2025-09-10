import { AsyncPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject } from '@angular/core';

import { SearchService } from '../../core/services/search.service';

@Component({
  selector: 'app-search-page',
  imports: [AsyncPipe],
  templateUrl: './search-page.html',
  styleUrl: './search-page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class SearchPage {
  private search = inject(SearchService);
  query$ = this.search.query$;
}
