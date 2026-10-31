import { Routes } from '@angular/router';

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
