import { Routes } from '@angular/router';

import { lazyUnsavedChangesGuard } from '../../core/layout/lazy-unsaved-changes-guard';

/** The pages of the squad, under `leagues/:leagueId`. */
export const squadRoutes: Routes = [
  {
    path: 'squad',
    loadComponent: () => import('./squad-page').then((m) => m.SquadPage),
    title: $localize`:@@squad.squad.pageTitle:Plantilla`,
  },
  {
    path: 'squad/:playerId',
    loadComponent: () => import('./player-page').then((m) => m.PlayerPage),
    title: $localize`:@@squad.player.pageTitle:Jugador`,
  },
];

/** The admin pages of the squad, under `leagues/:leagueId/admin`. */
export const squadAdminRoutes: Routes = [
  {
    path: 'squad',
    loadComponent: () => import('./admin/admin-squad-page').then((m) => m.AdminSquadPage),
    title: $localize`:@@squad.admin.squad.pageTitle:Jugadores`,
  },
  {
    path: 'squad/new',
    loadComponent: () => import('./admin/admin-player-page').then((m) => m.AdminPlayerPage),
    canDeactivate: [lazyUnsavedChangesGuard],
    title: $localize`:@@squad.admin.player.newPageTitle:Nuevo jugador`,
  },
  {
    path: 'squad/:playerId',
    loadComponent: () => import('./admin/admin-player-page').then((m) => m.AdminPlayerPage),
    canDeactivate: [lazyUnsavedChangesGuard],
    title: $localize`:@@squad.admin.player.pageTitle:Editar jugador`,
  },
];
