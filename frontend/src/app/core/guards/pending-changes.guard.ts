import { CanDeactivateFn } from '@angular/router';

export interface PendingChangesComponent {
  hasPendingChanges: () => boolean;
}

export const pendingChangesGuard: CanDeactivateFn<PendingChangesComponent> = (component) => {
  return component.hasPendingChanges ? !component.hasPendingChanges() || confirm('Discard changes?') : true;
};
