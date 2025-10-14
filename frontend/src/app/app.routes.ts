import { Routes } from '@angular/router';

import { authGuard } from '@core/guards/auth-guard';
import { loggedInGuard } from '@core/guards/logged-in.guard';
import { pendingChangesGuard } from '@core/guards/pending-changes.guard';
import { dreamResolver } from '@core/resolvers/dream.resolver';

import { AppShell } from './core/app-shell';
import { CalendarPage } from './features/calendar/calendar-page';
import { DreamDetail } from './features/dreams/dream-detail';
import { DreamEdit } from './features/dreams/dream-edit';
import { DreamList } from './features/dreams/dream-list';
import { LandingPageComponent } from './features/landing/landing-page.component';
import { LoginPageComponent } from './features/login/login-page.component';
import { NotFoundPage } from './features/not-found/not-found-page';
import { SearchPage } from './features/search/search-page';
import { SettingsPage } from './features/settings/settings-page';

export const routes: Routes = [
  { path: '', component: LandingPageComponent, canActivate: [loggedInGuard] },
  { path: 'login', component: LoginPageComponent, canActivate: [loggedInGuard] },
  {
    path: 'app',
    component: AppShell,
    canActivate: [authGuard],
    children: [
      { path: '', redirectTo: 'dreams', pathMatch: 'full' },
      { path: 'dreams', component: DreamList },
      { path: 'dreams/new', component: DreamEdit, canDeactivate: [pendingChangesGuard] },
      { path: 'dreams/:id', component: DreamDetail, resolve: { dream: dreamResolver } },
      { path: 'dreams/:id/edit', component: DreamEdit, canDeactivate: [pendingChangesGuard] },
      { path: 'search', component: SearchPage },
      { path: 'calendar', component: CalendarPage },
      { path: 'settings', component: SettingsPage },
      { path: '**', component: NotFoundPage },
    ],
  },
  { path: '**', component: NotFoundPage },
];
