import { ChangeDetectionStrategy, Component, Input } from '@angular/core';

@Component({
  selector: 'app-empty-state',
  imports: [],
  templateUrl: './empty-state.html',
  styleUrl: './empty-state.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class EmptyState {
  @Input() message = 'Nothing here yet';
}
