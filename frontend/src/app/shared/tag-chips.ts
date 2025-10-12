import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { MatChipsModule } from '@angular/material/chips';

@Component({
  selector: 'app-tag-chips',
  imports: [MatChipsModule, CommonModule],
  templateUrl: './tag-chips.html',
  styleUrls: ['./tag-chips.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TagChips {
  @Input() tags: string[] = [];
}
