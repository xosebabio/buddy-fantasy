import { Routes } from '@angular/router';

import { adminGuard, authGuard, memberGuard } from './core/auth/guards';
import { homeRedirect } from './core/auth/home-redirect';
import { ERROR_PAGES } from './core/pages/error-page-texts';
import { devRoutes } from './dev.routes';
import { identityRoutes, loginRoutes } from './features/identity/identity.routes';
import { leagueAdminRoutes, leagueRoutes, leaguesRoutes } from './features/league/league.routes';
import { legalRoutes } from './features/legal/legal.routes';
import { lineupRoutes } from './features/lineup/lineup.routes';
import { matchAdminRoutes, matchRoutes } from './features/match/match.routes';
import { ratingAdminRoutes, ratingRoutes } from './features/rating/rating.routes';
import { scoringRoutes } from './features/scoring/scoring.routes';
import { squadAdminRoutes, squadRoutes } from './features/squad/squad.routes';

// Lazy as well, to keep the initial bundle under its budget (RNF-13).
const shell = () => import('./core/layout/shell').then((m) => m.Shell);
const errorPage = () => import('./core/pages/error-page').then((m) => m.ErrorPage);

/**
 * The routes of the app (FE-03), in English and with the league in the URL. The pages of each
 * feature are lazy and go inside the shell: those outside a league first (so that `leagues/new`
 * is not taken for a league), then those of a league under `leagues/:leagueId`, with its admin
 * ones under `admin`. The login and the legal pages have the whole screen, and the legal ones
 * need no session.
 */
export const routes: Routes = [
  ...devRoutes,
  { path: '', pathMatch: 'full', redirectTo: homeRedirect },
  ...loginRoutes,
  ...legalRoutes,
  {
    path: '',
    loadComponent: shell,
    children: [...leaguesRoutes, ...identityRoutes],
  },
  {
    path: 'leagues/:leagueId',
    loadComponent: shell,
    canActivate: [authGuard, memberGuard],
    children: [
      ...leagueRoutes,
      ...squadRoutes,
      ...lineupRoutes,
      ...matchRoutes,
      ...ratingRoutes,
      ...scoringRoutes,
      {
        path: 'admin',
        canActivate: [adminGuard],
        children: [
          ...leagueAdminRoutes,
          ...squadAdminRoutes,
          ...matchAdminRoutes,
          ...ratingAdminRoutes,
        ],
      },
    ],
  },
  {
    path: '',
    loadComponent: shell,
    children: [
      {
        path: 'forbidden',
        loadComponent: errorPage,
        data: { kind: 'forbidden' },
        title: ERROR_PAGES.forbidden.title,
      },
      {
        path: 'not-found',
        loadComponent: errorPage,
        data: { kind: 'not-found' },
        title: ERROR_PAGES['not-found'].title,
      },
      {
        path: '**',
        loadComponent: errorPage,
        data: { kind: 'not-found' },
        title: ERROR_PAGES['not-found'].title,
      },
    ],
  },
];
