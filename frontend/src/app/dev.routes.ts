import { Routes } from '@angular/router';

// Development-only routes; the production build swaps this file for dev.routes.prod.ts.
export const devRoutes: Routes = [
  {
    path: '_ui',
    loadComponent: () => import('./ui/catalog/catalog-page').then((m) => m.CatalogPage),
  },
];
