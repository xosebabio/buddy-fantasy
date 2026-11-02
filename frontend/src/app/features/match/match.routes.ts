import { Routes } from '@angular/router';

import { lazyUnsavedChangesGuard } from '../../core/layout/lazy-unsaved-changes-guard';

/** The pages of the calendar, under `leagues/:leagueId`. */
export const matchRoutes: Routes = [
  {
    path: 'matches',
    loadComponent: () => import('./matches-page').then((m) => m.MatchesPage),
    title: $localize`:@@match.matches.pageTitle:Calendario`,
  },
  {
    path: 'matches/:matchId',
    loadComponent: () => import('./match-page').then((m) => m.MatchPage),
    title: $localize`:@@match.match.pageTitle:Partido`,
  },
];

/** The admin pages of the calendar, under `leagues/:leagueId/admin`. */
export const matchAdminRoutes: Routes = [
  {
    path: 'matches',
    loadComponent: () => import('./admin/admin-matches-page').then((m) => m.AdminMatchesPage),
    title: $localize`:@@match.admin.matches.pageTitle:Partidos`,
  },
  {
    path: 'matches/new',
    loadComponent: () => import('./admin/admin-match-form-page').then((m) => m.AdminMatchFormPage),
    canDeactivate: [lazyUnsavedChangesGuard],
    title: $localize`:@@match.admin.form.newPageTitle:Programar partido`,
  },
  {
    path: 'matches/:matchId',
    loadComponent: () => import('./admin/admin-match-page').then((m) => m.AdminMatchPage),
    title: $localize`:@@match.admin.match.pageTitle:Partido`,
  },
  {
    path: 'matches/:matchId/edit',
    loadComponent: () => import('./admin/admin-match-form-page').then((m) => m.AdminMatchFormPage),
    canDeactivate: [lazyUnsavedChangesGuard],
    title: $localize`:@@match.admin.form.pageTitle:Editar partido`,
  },
];
