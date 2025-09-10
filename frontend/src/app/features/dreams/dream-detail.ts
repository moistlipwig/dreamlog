import { ChangeDetectionStrategy, Component } from '@angular/core';
import { AsyncPipe } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { switchMap } from 'rxjs';
import { DreamsService } from '../../core/services/dreams.service';
import { TagChips } from '../../shared/tag-chips';

@Component({
  selector: 'app-dream-detail',
  imports: [AsyncPipe, MatButtonModule, RouterLink, TagChips],
  templateUrl: './dream-detail.html',
  styleUrl: './dream-detail.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class DreamDetail {
  dream$ = this.route.paramMap.pipe(
    switchMap((params) => this.dreams.get(params.get('id')!))
  );

  constructor(private route: ActivatedRoute, private dreams: DreamsService) {}
}
