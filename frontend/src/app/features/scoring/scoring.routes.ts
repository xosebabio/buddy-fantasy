import { Routes } from '@angular/router';

/** The pages of the points, under `leagues/:leagueId`. */
export const scoringRoutes: Routes = [
  {
    path: 'standings',
    loadComponent: () => import('./standings-page').then((m) => m.StandingsPage),
    title: $localize`:@@scoring.standings.pageTitle:Clasificación`,
  },
  {
    path: 'standings/:matchId',
    loadComponent: () => import('./matchday-page').then((m) => m.MatchdayPage),
    title: $localize`:@@scoring.matchday.pageTitle:Puntos de la jornada`,
  },
  {
    path: 'managers/:userId',
    loadComponent: () => import('./manager-history-page').then((m) => m.ManagerHistoryPage),
    title: $localize`:@@scoring.history.pageTitle:Historial de un manager`,
  },
];
