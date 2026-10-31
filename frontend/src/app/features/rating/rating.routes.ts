import { Routes } from '@angular/router';

import { lazyUnsavedChangesGuard } from '../../core/layout/lazy-unsaved-changes-guard';

/** The pages of the ratings, under `leagues/:leagueId`. */
export const ratingRoutes: Routes = [
  {
    path: 'ratings',
    loadComponent: () => import('./ratings-page').then((m) => m.RatingsPage),
    title: $localize`:@@rating.ratings.pageTitle:Valoraciones`,
  },
  {
    path: 'ratings/:matchId',
    loadComponent: () => import('./rate-match-page').then((m) => m.RateMatchPage),
    canDeactivate: [lazyUnsavedChangesGuard],
    title: $localize`:@@rating.rate.pageTitle:Valorar un partido`,
  },
];
