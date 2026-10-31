import { Routes } from '@angular/router';

import { adminGuard, authGuard, memberGuard } from './core/auth/guards';
import { homeRedirect } from './core/auth/home-redirect';
import { ERROR_PAGES } from './core/pages/error-page-texts';
import { devRoutes } from './dev.routes';

// Lazy as well, to keep the initial bundle under its budget (RNF-13).
const shell = () => import('./core/layout/shell').then((m) => m.Shell);
const errorPage = () => import('./core/pages/error-page').then((m) => m.ErrorPage);

/**
 * The routes of the app (FE-03), in English and with the league in the URL. The pages of each
 * feature are lazy and go inside the shell: those of a league under `leagues/:leagueId`, and its
 * admin ones under `admin`.
 */
export const routes: Routes = [
  ...devRoutes,
  { path: '', pathMatch: 'full', redirectTo: homeRedirect },
  {
    path: 'leagues/:leagueId',
    loadComponent: shell,
    canActivate: [authGuard, memberGuard],
    children: [{ path: 'admin', canActivate: [adminGuard], children: [] }],
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
