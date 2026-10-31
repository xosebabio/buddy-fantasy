import { Routes } from '@angular/router';

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
