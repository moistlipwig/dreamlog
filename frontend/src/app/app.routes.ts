import { Routes } from '@angular/router';

import { authGuard } from './core/guards/auth-guard';
import { pendingChangesGuard } from './core/guards/pending-changes.guard';
import { CalendarPage } from './features/calendar/calendar-page';
import { DreamDetail } from './features/dreams/dream-detail';
import { DreamEdit } from './features/dreams/dream-edit';
import { DreamList } from './features/dreams/dream-list';
import { Login } from './features/login/login';
import { NotFoundPage } from './features/not-found/not-found-page';
import { SearchPage } from './features/search/search-page';
import { SettingsPage } from './features/settings/settings-page';

export const routes: Routes = [
  { path: 'login', component: Login },
  {
    path: '',
    canActivate: [authGuard],
    children: [
      { path: '', redirectTo: 'dreams', pathMatch: 'full' },
      { path: 'dreams', component: DreamList },
      { path: 'dreams/new', component: DreamEdit, canDeactivate: [pendingChangesGuard] },
      { path: 'dreams/:id', component: DreamDetail },
      { path: 'dreams/:id/edit', component: DreamEdit, canDeactivate: [pendingChangesGuard] },
      { path: 'search', component: SearchPage },
      { path: 'calendar', component: CalendarPage },
      { path: 'settings', component: SettingsPage }
    ]
  },
  { path: '**', component: NotFoundPage }
];
