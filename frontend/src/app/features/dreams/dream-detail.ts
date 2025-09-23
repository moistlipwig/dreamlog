import { AsyncPipe, CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { switchMap } from 'rxjs';

import { DreamsService } from '../../core/services/dreams.service';
import { TagChips } from '../../shared/tag-chips';

@Component({
  selector: 'app-dream-detail',
  imports: [AsyncPipe, MatButtonModule, MatCardModule, RouterLink, TagChips, CommonModule],
  templateUrl: './dream-detail.html',
  styleUrls: ['./dream-detail.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DreamDetail {
  private route = inject(ActivatedRoute);
  private dreams = inject(DreamsService);

  dream$ = this.route.paramMap.pipe(switchMap((params) => this.dreams.get(params.get('id')!)));
}
