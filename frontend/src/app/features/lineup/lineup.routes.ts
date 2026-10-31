import { Routes } from '@angular/router';

import { lazyUnsavedChangesGuard } from '../../core/layout/lazy-unsaved-changes-guard';

/** The pages of the lineup, under `leagues/:leagueId`. */
export const lineupRoutes: Routes = [
  {
    path: 'lineup',
    loadComponent: () => import('./lineup-page').then((m) => m.LineupPage),
    canDeactivate: [lazyUnsavedChangesGuard],
    title: $localize`:@@lineup.lineup.pageTitle:Alineación`,
  },
  {
    path: 'lineups/:matchId',
    loadComponent: () => import('./locked-lineups-page').then((m) => m.LockedLineupsPage),
    title: $localize`:@@lineup.locked.pageTitle:Alineaciones bloqueadas`,
  },
];
