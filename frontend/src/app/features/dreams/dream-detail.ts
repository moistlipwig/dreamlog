import { AsyncPipe, DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { map } from 'rxjs';

import { Dream, Mood } from '../../core/models/dream';
import { TagChips } from '../../shared/tag-chips';

@Component({
  selector: 'app-dream-detail',
  imports: [AsyncPipe, MatButtonModule, MatCardModule, RouterLink, TagChips, DatePipe],
  templateUrl: './dream-detail.html',
  styleUrls: ['./dream-detail.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DreamDetail {
  private readonly route = inject(ActivatedRoute);

  dream$ = this.route.data.pipe(map((data) => data['dream'] as Dream | null));

  /**
   * Format Mood enum to human-readable string.
   */
  formatMood(mood: Mood): string {
    return mood.charAt(0) + mood.slice(1).toLowerCase();
  }
}
