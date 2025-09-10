import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { MatChipsModule } from '@angular/material/chips';

@Component({
  selector: 'app-tag-chips',
  imports: [MatChipsModule],
  templateUrl: './tag-chips.html',
  styleUrl: './tag-chips.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class TagChips {
  @Input() tags: string[] = [];
}
