import { AsyncPipe, CommonModule, DatePipe, NgFor } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { RouterLink } from '@angular/router';

import { DreamsService } from '../../core/services/dreams.service';
import { TagChips } from '../../shared/tag-chips';

@Component({
  selector: 'app-dream-list',
  imports: [NgFor, AsyncPipe, MatCardModule, RouterLink, TagChips, DatePipe, CommonModule],
  templateUrl: './dream-list.html',
  styleUrls: ['./dream-list.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class DreamList {
  private dreams = inject(DreamsService);
  dreams$ = this.dreams.list();
}
