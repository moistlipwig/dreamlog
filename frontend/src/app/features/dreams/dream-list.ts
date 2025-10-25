import { AsyncPipe, DatePipe, SlicePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { RouterLink } from '@angular/router';
import { map } from 'rxjs';

import { DreamsService } from '../../core/services/dreams.service';
import { EmptyState } from '../../shared/empty-state';
import { TagChips } from '../../shared/tag-chips';

@Component({
  selector: 'app-dream-list',
  imports: [AsyncPipe, MatCardModule, RouterLink, TagChips, DatePipe, SlicePipe, EmptyState],
  templateUrl: './dream-list.html',
  styleUrls: ['./dream-list.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DreamList {
  private dreamsService = inject(DreamsService);
  dreams$ = this.dreamsService.list().pipe(map((page) => page.content));
  skeletons = Array.from({ length: 4 });
}
